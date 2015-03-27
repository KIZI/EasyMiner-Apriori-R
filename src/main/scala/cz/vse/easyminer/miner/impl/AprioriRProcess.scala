package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.ARule
import cz.vse.easyminer.miner.Attribute
import cz.vse.easyminer.miner.BadOutputData
import cz.vse.easyminer.miner.BoolExpression
import cz.vse.easyminer.miner.Confidence
import cz.vse.easyminer.miner.ContingencyTable
import cz.vse.easyminer.miner.Count
import cz.vse.easyminer.miner.Dataset
import cz.vse.easyminer.miner.DatasetBuilder
import cz.vse.easyminer.miner.DatasetQueryBuilder
import cz.vse.easyminer.miner.FixedValue
import cz.vse.easyminer.miner.Lift
import cz.vse.easyminer.miner.Limit
import cz.vse.easyminer.miner.MinerProcess
import cz.vse.easyminer.miner.MinerTask
import cz.vse.easyminer.miner.MinerTaskValidator
import cz.vse.easyminer.miner.RConnectionPool
import cz.vse.easyminer.miner.RScript
import cz.vse.easyminer.miner.Support
import cz.vse.easyminer.miner.Value
import cz.vse.easyminer.util.AnyToDouble
import cz.vse.easyminer.util.Template
import org.slf4j.LoggerFactory
import scala.annotation.tailrec

class AprioriRProcess(
  rTemplate: String,
  jdbcDriverAbsolutePath: String,
  val rcp: RConnectionPool
) extends MinerProcess with RScript {

  self: DBOpts with DatasetBuilder with DatasetQueryBuilder with MinerTaskValidator =>

  import cz.vse.easyminer.util.BasicFunction._

  val defaultSupport = 0.001
  val defaultConfidence = 0.1
  val logger = LoggerFactory.getLogger("cz.vse.easyminer.miner.impl.AprioriRProcess")

  private def executeQueries[T]: (Dataset => T) => T = {
    buildAndExecute(dbServer, dbName, dbUser, dbPass, dbTableName) _
  }

  private def getInputRValues(exp: BoolExpression[Attribute])(implicit db: Dataset) = toSQLSelectMap(exp)
    .flatMap { case (k, v) => db.fetchValuesBySelectAndColName(v, k) collect { case (Some(v)) => "\"" + s"$k=$v" + "\"" } }
    .mkString(", ")

  private def getInputSelectQuery(exp: BoolExpression[Attribute]) = toSQLSelect(exp)
    .mkString(", ")

  private def getOutputARuleMapper(count: Count)(implicit db: Dataset) = {
    val ArulePattern = """\d+\s+\{(.*?)\}\s+=>\s+\{(.+?)\}\s+([0-9.]+)\s+([0-9.]+)\s+([0-9.]+)""".r
    implicit val cache = collection.mutable.Map.empty[RArule.State, Boolean]
    val pf: PartialFunction[String, ARule] = {
      case ArulePattern(RArule(ant), RArule(Some(con)), AnyToDouble(s), AnyToDouble(c), AnyToDouble(l)) => {
        val (supp, conf, lift) = (Support(s), Confidence(c), Lift(l))
        ARule(ant, con, Set(supp, conf, lift, count), ContingencyTable(supp, conf, lift, count))
      }
    }
    pf
  }

  protected def innerMine(mt: MinerTask) = executeQueries { implicit db =>
    import cz.vse.easyminer.util.BasicFunction._
    logger.debug(s"New task was received: $mt")
    val im = mt.interestMeasures.foldLeft(Map("confidence" -> defaultConfidence, "support" -> defaultSupport)) {
      case (m, Confidence(x)) => m + ("confidence" -> x)
      case (m, Support(x)) => m + ("support" -> x)
      case (m, Lift(x)) => m + ("lift" -> x)
      case (m, Limit(x)) => m + ("limit" -> x)
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
    val result = eval(rscript).collect(getOutputARuleMapper(count)).toSeq
    logger.debug(s"Number of found association rules: ${result.size}")
    result
  }

}

object RArule {

  sealed trait State
  case class SearchName(n: String) extends State
  case class SearchValue(n: String, v: String) extends State

  class RAruleParser(db: Dataset, cache: collection.mutable.Map[State, Boolean]) {

    private def checkName(name: String) = cache.getOrElseUpdate(SearchName(name), db.hasColName(name))

    private def checkValueName(name: String, value: String) = cache.getOrElseUpdate(SearchValue(name, value), db.hasValueByColName(name, value))

    @tailrec
    final def parseNextChar(str: String, v: State = SearchName(""), r: List[FixedValue] = Nil): List[FixedValue] = v match {
      case SearchName(n) => str.headOption match {
        case Some('=') =>
          if (checkName(n))
            parseNextChar(str.tail, SearchValue(n, ""), r)
          else
            parseNextChar(str.tail, SearchName(n + '='), r)
        case Some(x) => parseNextChar(str.tail, SearchName(n + x), r)
        case None => r match {
          case fv :: tailr => parseNextChar(n, SearchValue(fv.name, fv.value + ','), tailr)
          case Nil => throw new BadOutputData(s"Unparsable attribute in association rule: $n")
        }
      }
      case SearchValue(n, v) => str.headOption match {
        case Some(',') =>
          if (checkValueName(n, v))
            parseNextChar(str.tail, SearchName(""), FixedValue(n, v) :: r)
          else
            parseNextChar(str.tail, SearchValue(n, v + ','), r)
        case Some(x) => parseNextChar(str.tail, SearchValue(n, v + x), r)
        case None =>
          if (checkValueName(n, v))
            FixedValue(n, v) :: r
          else
            parseNextChar(v, SearchName(n + '='), r)
      }
    }

  }

  def unapply(str: String)(implicit db: Dataset, cache: collection.mutable.Map[State, Boolean]) = {
    if (str.isEmpty)
      Some(None)
    else {
      val arp = new RAruleParser(db, cache)
      arp.parseNextChar(str).map(x => Value(x).asInstanceOf[BoolExpression[FixedValue]]).reduceLeftOption(_ AND _).map(x => Some(x))
    }
  }

}