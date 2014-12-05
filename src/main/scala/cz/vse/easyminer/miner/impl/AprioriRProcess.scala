package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.ARule
import cz.vse.easyminer.miner.Attribute
import cz.vse.easyminer.miner.BoolExpression
import cz.vse.easyminer.miner.Confidence
import cz.vse.easyminer.miner.ContingencyTable
import cz.vse.easyminer.miner.Count
import cz.vse.easyminer.miner.Dataset
import cz.vse.easyminer.miner.DatasetBuilder
import cz.vse.easyminer.miner.DatasetQueryBuilder
import cz.vse.easyminer.miner.FixedValue
import cz.vse.easyminer.miner.Lift
import cz.vse.easyminer.miner.MinerProcess
import cz.vse.easyminer.miner.MinerTask
import cz.vse.easyminer.miner.Support
import cz.vse.easyminer.miner.Value
import cz.vse.easyminer.util.AnyToDouble
import cz.vse.easyminer.util.Template
import org.rosuda.REngine.Rserve.RConnection
import org.slf4j.LoggerFactory

class AprioriRProcess(
  val pmml: xml.Node,
  rTemplate: String,
  jdbcDriverAbsolutePath: String,
  rServer: String,
  rPort: Int = 6311
) extends MinerProcess {
  
  self: PMMLDB with DatasetBuilder with DatasetQueryBuilder =>
  
  import cz.vse.easyminer.util.BasicFunction._
  
  val defaultSupport = 0.001
  val defaultConfidence = 0.2
  val logger = LoggerFactory.getLogger("cz.vse.easyminer.miner.impl.AprioriRProcess")
  
  object RAruleToBoolExpression {
    def unapply(str: String) = str
    .split(',')
    .map(_.split("=", 2))
    .collect{
      case Array(k, v) => Value(FixedValue(k, v)) : BoolExpression[FixedValue]
    }
    .reduceLeftOption(_ AND _)
  }
  
  private def executeQueries[T]: (Dataset => T) => T = {
    buildAndExecute(dbServer, dbName, dbUser, dbPass, dbTableName) _
  }
  
  private def evalRScript(rscript: String) = tryCloseBool(new RConnection(rServer, rPort))(_.parseAndEval(rscript.trim.replaceAll("\r\n", "\n")).asStrings)
  
  private def getInputRValues(exp: BoolExpression[Attribute])(implicit db: Dataset) = toSQLSelectMap(exp)
  .flatMap{case (k, v) => db.fetchValuesBySelectAndColName(v, k) collect {case(Some(v)) => "\"" + s"$k=$v" + "\""}}
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
  
  def mine(mt: MinerTask) = executeQueries(implicit db => {
      import cz.vse.easyminer.util.BasicFunction._
      logger.debug(s"New task was received: $mt")
      val im = mt.interestMeasures.foldLeft(Map("confidence" -> defaultConfidence, "support" -> defaultSupport)) {
        case (m, Confidence(x)) => m + ("confidence" -> x)
        case (m, Support(x)) => m + ("support" -> x)
        case (m, Lift(x)) => m + ("lift" -> x)
        case (m, _) => m
      }
      val inputSelectQuery = getInputSelectQuery(mt.antecedent OR mt.consequent)
      val inputConsequentValues = getInputRValues(mt.consequent)
      logger.debug("Itemsets will be filtered by this SQL Select query: " + inputSelectQuery)
      logger.debug("Consequent values are: " + inputConsequentValues)
      val rscript = Template(
        rTemplate,
        Map(
          "jdbcDriverAbsolutePath" -> jdbcDriverAbsolutePath,
          "dbServer" -> dbServer,
          "dbName" -> dbName,
          "dbUser" -> dbUser,
          "dbPassword" -> dbPass,
          "dbTableName" -> dbTableName,
          "selectQuery" -> inputSelectQuery,
          "consequent" -> inputConsequentValues
        ) ++ im
      )
      logger.trace("This Rscript will be passed to the Rserve:\n" + rscript)
      val count = Count(db.fetchCount)
      val result = evalRScript(rscript).collect(getOutputARuleMapper(count)).toSeq
      logger.debug(s"Number of found association rules: ${result.size}")
      result
    }
  )
  
}