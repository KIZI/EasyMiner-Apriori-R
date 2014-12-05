package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.AllValues
import cz.vse.easyminer.miner.Attribute
import cz.vse.easyminer.miner.BadInputData
import cz.vse.easyminer.miner.BoolExpression
import cz.vse.easyminer.miner.Confidence
import cz.vse.easyminer.miner.FixedValue
import cz.vse.easyminer.miner.InterestMeasure
import cz.vse.easyminer.miner.Support
import cz.vse.easyminer.miner.Value
import cz.vse.easyminer.util.AnyToDouble
import cz.vse.easyminer.util.AnyToInt
import cz.vse.easyminer.util.Lift
import scalikejdbc._

class PMMLTask(pmml: xml.Node) {
  
  private val boolExpElemName = "DBASetting"
  private val attrElemName = "BBASetting"
  private val attrElemRefName = "BASettingRef"
  private val boolSignElemName = "LiteralSign"
  private val interestMeasureElemName = "InterestMeasure"
  private val interestThresholdElemName = "Threshold"
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
  
  def fetchAntecedent: BoolExpression[Attribute] = (pmml \\ "AntecedentSetting").headOption.map(x => findElemByElemId(x)(boolExpElemName) map toExpression) match {
    case Some(Some(x)) => x
    case _ => throw new BadInputData("Unparsable antecedent.")
  }
  
  def fetchConsequent: BoolExpression[Attribute] = (pmml \\ "ConsequentSetting").headOption.map(x => findElemByElemId(x)(boolExpElemName) map toExpression) match {
    case Some(Some(x)) => x
    case _ => throw new BadInputData("Unparsable consequent.")
  }
  
  def fetchInterestMeasures: Set[InterestMeasure] = (pmml \\ "InterestMeasureThreshold")
  .map(x => (x \ interestMeasureElemName).text -> (x \ interestThresholdElemName).text)
  .collect{
    case ("FUI", AnyToDouble(v)) => Confidence(v)
    case ("SUPP" | "BASE", AnyToDouble(v)) => Support(v)
  }
  .toSet

}
