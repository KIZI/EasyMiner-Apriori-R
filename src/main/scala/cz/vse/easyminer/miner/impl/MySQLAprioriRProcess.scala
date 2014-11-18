package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.AND
import cz.vse.easyminer.miner.ARule
import cz.vse.easyminer.miner.AllValues
import cz.vse.easyminer.miner.Attribute
import cz.vse.easyminer.miner.BadInputData
import cz.vse.easyminer.miner.BoolExpression
import cz.vse.easyminer.miner.Confidence
import cz.vse.easyminer.miner.ContingencyTable
import cz.vse.easyminer.miner.FixedValue
import cz.vse.easyminer.miner.Lift
import cz.vse.easyminer.miner.MinerProcess
import cz.vse.easyminer.miner.MinerResult
import cz.vse.easyminer.miner.MinerTask
import cz.vse.easyminer.miner.NOT
import cz.vse.easyminer.miner.OR
import cz.vse.easyminer.miner.Support
import cz.vse.easyminer.miner.Value
import cz.vse.easyminer.util.AnyToDouble
import cz.vse.easyminer.util.Template
import org.rosuda.REngine.Rserve.RConnection

class MySQLAprioriRProcess(pmml: scala.xml.Elem, rServer: String, rPort: Int = 6311) extends MinerProcess {
  
  val defaultSupport = 0.001
  val defaultConfidence = 0.2
  
  private val jdbcDriverAbsolutePath = "/home/venca/RWorks/mysql-jdbc";
  
  private val (dbServer, dbName, dbUser, dbPass, dbTableName) = {
    val extensions = (pmml \ "Header" \ "Extension").map(ext =>
      (ext \ "@name").text -> (ext \ "@value").text
    ).toMap
    List("database-server", "database-name", "database-user", "database-password", "dataset", "database-type").map(x => extensions.getOrElse(x, "")).filter(!_.isEmpty) match {
      case a @ List(dbServer, dbName, dbUser, dbPass, dbTableName, "mysql") => (dbServer, dbName, dbUser, dbPass, dbTableName)
      case x => throw new BadInputData(s"Missing data to prepare dataset. Given: $x")
    }
  }
  
  def prepareDataset[T]: (MySQLDataset => T) => T = {
    MySQLDataset.apply(dbServer, dbName, dbUser, dbPass, dbTableName) _
  }
  
  private def joinMaps(m1 : Map[String, String], m2 : Map[String, String], f: (String, String) => String) = m1.foldLeft(m2){
    case (r, t @ (k, _)) if !r.contains(k) => r + t
    case (r, (k, v)) => {
        val rv = r(k)
        r + (k -> (if (rv.isEmpty || v.isEmpty) "" else f(rv, v))) 
      }
  }
    
  private def toSQLMap(exp: BoolExpression[Attribute]) : Map[String, String] = exp match {
    case AND(a, b) => joinMaps(toSQLMap(a), toSQLMap(b), (a, b) => s"($a AND $b)")
    case OR(a, b) => joinMaps(toSQLMap(a), toSQLMap(b), (a, b) => s"($a OR $b)")
    case Value(AllValues(a)) => Map(a -> "")
    case Value(FixedValue(a, v)) => Map(a -> s"`$a` = '$v'")
    case NOT(a) => toSQLMap(a) map {case(k, v) => k -> (if (v.isEmpty) v else s"NOT($v)")}
    case _ => Map.empty
  }
  
  private val mapToSQLSelect : PartialFunction[(String, String), String] = {
    case(k, "") => k
    case(k, v) => s"IF($v, `$k`, NULL) AS `$k`"
  }
  
  private def toSQLSelect(exp: BoolExpression[Attribute]) : String = {
    toSQLMap(exp)
    .map(mapToSQLSelect)
    .mkString(", ")
  }
  
  private def toRValues(exp: BoolExpression[Attribute]) = prepareDataset(mysql =>
    toSQLMap(exp)
    .map(kv => mysql.fetchValuesBySelectAndColName(mapToSQLSelect(kv), kv._1) collect {case(Some(v)) => "\"" + s"${kv._1}=$v" + "\""})
    .flatMap(x => x)
    .mkString(", ")
  )
  
  object ANDOR {
    def unapply(exp: BoolExpression[Attribute]) = exp match {
      case AND(a, b) => Some(a, b)
      case OR(a, b) => Some(a, b)
      case _ => None
    }
  }
  
  object RresultToBoolExpression {
    def unapply(str: String) = str
    .split(',')
    .map(_.split("=", 2))
    .collect{
      case Array(k, v) => Value(FixedValue(k, v)) : BoolExpression[FixedValue]
    }
    .reduceLeftOption(_ AND _)
  }
  
  def mine(mt: MinerTask) : MinerResult = {
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
        "selectQuery" -> toSQLSelect(mt.antecedent OR mt.consequent),
        "consequent" -> toRValues(mt.consequent)
      ) ++ im
    )
    tryCloseBool(new RConnection(rServer, rPort))(conn => {
        val ArulePattern = """\d+\s+\{(.+)\}\s+=>\s+\{(.+)\}\s+([0-9.]+)\s+([0-9.]+)\s+([0-9.]+)""".r
        MinerResult(
          conn
          .parseAndEval(rscript.trim.replaceAll("\r\n", "\n"))
          .asStrings
          .collect {
            case ArulePattern(RresultToBoolExpression(ant), RresultToBoolExpression(con), AnyToDouble(supp), AnyToDouble(conf), AnyToDouble(lift)) =>
              ARule(ant, con, Set(Support(supp), Confidence(conf), Lift(lift)), ContingencyTable(0, 0, 0, 0))
          }
          .toSeq
        )
      }
    )
  }
  
}
