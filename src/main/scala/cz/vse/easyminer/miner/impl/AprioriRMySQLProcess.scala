package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.ARule
import cz.vse.easyminer.miner.Attribute
import cz.vse.easyminer.miner.BoolExpression
import cz.vse.easyminer.miner.Confidence
import cz.vse.easyminer.miner.ContingencyTable
import cz.vse.easyminer.miner.Count
import cz.vse.easyminer.miner.FixedValue
import cz.vse.easyminer.miner.Lift
import cz.vse.easyminer.miner.MinerProcess
import cz.vse.easyminer.miner.MinerTask
import cz.vse.easyminer.miner.Support
import cz.vse.easyminer.miner.Value
import cz.vse.easyminer.util.AnyToDouble
import cz.vse.easyminer.util.Template
import org.rosuda.REngine.Rserve.RConnection

class AprioriRMySQLProcess(rServer: String, rPort: Int = 6311) extends MinerProcess {
  
  self: PMMLMySQL =>
  
  import MySQLQueryBuilder._
  import cz.vse.easyminer.util.BasicFunction._
  
  val defaultSupport = 0.001
  val defaultConfidence = 0.2
  
  private val jdbcDriverAbsolutePath = "/home/venca/RWorks/mysql-jdbc";
  
  object RAruleToBoolExpression {
    def unapply(str: String) = str
    .split(',')
    .map(_.split("=", 2))
    .collect{
      case Array(k, v) => Value(FixedValue(k, v)) : BoolExpression[FixedValue]
    }
    .reduceLeftOption(_ AND _)
  }
  
  private def executeMySQLQueries[T]: (MySQLDataset => T) => T = {
    MySQLDataset.apply(dbServer, dbName, dbUser, dbPass, dbTableName) _
  }
  
  private def evalRScript(rscript: String) = tryCloseBool(new RConnection(rServer, rPort))(_.parseAndEval(rscript.trim.replaceAll("\r\n", "\n")).asStrings)
  
  private def getInputRValues(exp: BoolExpression[Attribute])(implicit mysql: MySQLDataset) = toSQLSelectMap(exp)
  .flatMap{case (k, v) => mysql.fetchValuesBySelectAndColName(v, k) collect {case(Some(v)) => "\"" + s"$k=$v" + "\""}}
  .mkString(", ")
  
  private def getInputSelectQuery(exp: BoolExpression[Attribute]) = toSQLSelect(exp)
  .mkString(", ")
  
  private def getOutputARuleMapper(count: Count) = {
    val ArulePattern = """\d+\s+\{(.+)\}\s+=>\s+\{(.+)\}\s+([0-9.]+)\s+([0-9.]+)\s+([0-9.]+)""".r
    val pf : PartialFunction[String, ARule] = {
      case ArulePattern(RAruleToBoolExpression(ant), RAruleToBoolExpression(con), AnyToDouble(s), AnyToDouble(c), AnyToDouble(l)) => {
          val (supp, conf, lift) = (Support(s), Confidence(c), Lift(l))
          ARule(ant, con, Set(supp, conf, lift, count), ContingencyTable(supp, conf, lift, count))
        }
    }
    pf
  }
  
  def mine(mt: MinerTask) = executeMySQLQueries(implicit mysql => {
      import cz.vse.easyminer.util.BasicFunction._
      val im = mt.interestMeasures.foldLeft(Map("confidence" -> defaultConfidence, "support" -> defaultSupport)) {
        case (m, Confidence(x)) => m + ("confidence" -> x)
        case (m, Support(x)) => m + ("support" -> x)
        case (m, Lift(x)) => m + ("lift" -> x)
        case (m, _) => m
      }
      val rscript = Template(
        "RAprioriWithMySQL.mustache",
        Map(
          "jdbcDriverAbsolutePath" -> jdbcDriverAbsolutePath,
          "dbServer" -> dbServer,
          "dbName" -> dbName,
          "dbUser" -> dbUser,
          "dbPassword" -> dbPass,
          "dbTableName" -> dbTableName,
          "selectQuery" -> getInputSelectQuery(mt.antecedent OR mt.consequent),
          "consequent" -> getInputRValues(mt.consequent)
        ) ++ im
      )
      val count = Count(mysql.fetchCount)
      evalRScript(rscript).collect(getOutputARuleMapper(count)).toSeq
    }
  )
  
}