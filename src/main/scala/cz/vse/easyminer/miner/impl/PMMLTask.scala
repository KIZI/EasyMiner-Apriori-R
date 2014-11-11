package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.AllValues
import cz.vse.easyminer.miner.Attribute
import cz.vse.easyminer.miner.BadInputData
import cz.vse.easyminer.miner.BoolExpression
import cz.vse.easyminer.miner.FixedValue
import cz.vse.easyminer.miner.MinerTask
import cz.vse.easyminer.miner.Value
import cz.vse.easyminer.miner.*
import cz.vse.easyminer.util.AnyToInt
import cz.vse.easyminer.util.Lift
import java.util.UUID
import scala.collection.immutable.Queue
import scalikejdbc._

object PMMLMySQLTask {
  


  
}

class PMMLMySQLTask(pmml: scala.xml.Elem) {
  
  private val boolExpElemName = "DBASetting"
  private val attrElemName = "BBASetting"
  private val attrElemRefName = "BASettingRef"
  private val boolSignElemName = "LiteralSign"
  private val optToExp: PartialFunction[Option[xml.Node], BoolExpression[Attribute]] = {case Some(x) => toExpression(x)}
  
  private def getElementById(name: String, id: Int) = pmml \\ name find (x => (x \ "@id").text == id.toString)
  
  private def findElemByElemId(el: xml.Node)(implicit elt: String) = Lift(el.text) {
    case AnyToInt(id) => getElementById(elt, id)
  }
  
  private def toExpression(el: xml.Node): BoolExpression[Attribute] = el.label match {
    case `boolExpElemName` => {
        val rs = (el \ attrElemRefName)
        val elt = (el \ "@type").text
        def nextExps(implicit elt: String) = rs map findElemByElemId collect optToExp
        if (rs.size > 1) {
          implicit val tn = boolExpElemName
          val joinExp = elt match {
            case "Disjunction" => (e1: BoolExpression[Attribute], e2: BoolExpression[Attribute]) => e1 OR e2
            case _ => (e1: BoolExpression[Attribute], e2: BoolExpression[Attribute]) => e1 AND e2
          }
          nextExps reduceLeft joinExp
        } else if (rs.size == 1 && elt == "Literal") {
          implicit val tn = attrElemName
          (el \ boolSignElemName).text match {
            case "Negative" => nextExps.head.NOT 
            case _ => nextExps.head
          }
        } else {
          implicit val tn = boolExpElemName
          nextExps.head
        }
      }
    case `attrElemName` => {
        val attrName = (el \ "FieldRef").text
        (el \ "Coefficient") match {
          case el if (el \ "Type").text == "One category" => Value(FixedValue(attrName, (el \ "Category").text))
          case _ => Value(AllValues(attrName))
        }
      }
    case elabel => throw new BadInputData(s"Unspecified element label: $elabel")
  }
  
  def prepareDataset[T]: (MySQLDataset => T) => T = {
    val extensions = (pmml \ "Header" \ "Extension").map(ext =>
      (ext \ "@name").text -> (ext \ "@value").text
    ).toMap
    List("database-server", "database-name", "database-user", "database-password", "dataset", "database-type").map(x => extensions.getOrElse(x, "")).filter(!_.isEmpty) match {
      case a @ List(dbServer, dbName, dbUser, dbPass, dbTableName, "mysql") => MySQLDataset.apply(dbServer, dbName, dbUser, dbPass, dbTableName) _
      case x => throw new BadInputData(s"Missing data to prepare dataset. Given: $x")
    }
  }
  
  def fetchAntecedent(implicit dataset: MySQLDataset): BoolExpression[Attribute] = Value(*)
  
  def fetchConsequent: BoolExpression[Attribute] = (pmml \\ "ConsequentSetting").headOption.map(x => findElemByElemId(x)(boolExpElemName) map toExpression) match {
    case Some(Some(x)) => x
    case _ => throw new BadInputData("Unparsable consequent.")
  }

}
