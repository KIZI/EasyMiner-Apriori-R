package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.ANDOR
import cz.vse.easyminer.miner.ARule
import cz.vse.easyminer.miner.ARuleVisualizer
import cz.vse.easyminer.miner.BoolExpression
import cz.vse.easyminer.miner.BoolExpressionVisualizer
import cz.vse.easyminer.miner.Confidence
import cz.vse.easyminer.miner.Count
import cz.vse.easyminer.miner.FixedValue
import cz.vse.easyminer.miner.Lift
import cz.vse.easyminer.miner.NOT
import cz.vse.easyminer.miner.Support
import cz.vse.easyminer.miner.Value
import cz.vse.easyminer.util.Template

class PMMLResult {
  
  self: ARuleVisualizer with BoolExpressionVisualizer =>
  
  private val arulesToPMMLMapper : PartialFunction[ARule, Map[String, Any]] = {
    case ar @ ARule(ant, con, im, ct) => Map(
        "id" -> s"AR${ar.hashCode}",
        "id-antecedent" -> baref(ant),
        "id-consequent" -> baref(con),
        "text" -> aruleToString(ar),
        "conf" -> im.collectFirst{case Confidence(conf) => conf},
        "supp" -> im.collectFirst{case Support(supp) => supp},
        "lift" -> im.collectFirst{case Lift(lift) => lift},
        "a" -> ct.a,
        "b" -> ct.b,
        "c" -> ct.c,
        "d" -> ct.d,
        "r" -> (ct.a + ct.b),
        "n" -> im.collectFirst{case Count(n) => n}
      )
  }
  
  private val dbaToPMMLMapper = {
    def makeDbaMap(expr: BoolExpression[FixedValue], children: List[String]) = Map(
      "id" -> baref(expr),
      "text" -> exprToString(expr),
      "barefs" -> children
    )
    val pf : PartialFunction[BoolExpression[FixedValue], Map[String, Any]] = {
      case e @ ANDOR(x, y) => makeDbaMap(e, List(baref(x), baref(y)))
      case e @ NOT(x) => makeDbaMap(e, List(baref(x)))
    }
    pf
  }
  
  private val bbaToPMMLMapper : PartialFunction[BoolExpression[FixedValue], Map[String, Any]] = {
    case e @ Value(x) => Map(
        "id" -> baref(e),
        "text" -> exprToString(e),
        "name" -> x.name,
        "value" -> x.value
      )
  }
  
  private def baref(expr: BoolExpression[FixedValue]) = expr match {
    case Value(_) => s"BBA${expr.hashCode}"
    case _ => s"DBA${expr.hashCode}"
  }
  
  private def collectExpression(be: BoolExpression[FixedValue]) : Set[BoolExpression[FixedValue]] = be match {
    case ANDOR(x, y) => (collectExpression(x) ++ collectExpression(y)) + be
    case NOT(x) => collectExpression(x) + be
    case _ => Set(be)
  }
  
  def toPMML(arules: Seq[ARule]) = {
    val exprs = arules.flatMap(x => Seq(x.antecedent, x.consequent)).map(collectExpression).reduceOption(_ ++ _)
    Template.apply(
      "PMMLResult.template.mustache",
      Map(
        "arules" -> arules.collect(arulesToPMMLMapper),
        "dbas" -> exprs.getOrElse(Nil).collect(dbaToPMMLMapper),
        "bbas" -> exprs.getOrElse(Nil).collect(bbaToPMMLMapper),
        "number-of-rules" -> arules.size
      )
    )
  }
  
}
