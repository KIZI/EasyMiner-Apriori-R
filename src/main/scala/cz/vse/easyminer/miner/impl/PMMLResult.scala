package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.{ANDOR, ARule, ARuleVisualizer, BoolExpression, BoolExpressionVisualizer, FixedValue, NOT, Value}
import cz.vse.easyminer.util.Template

class PMMLResult(arules: Seq[ARule]) {

  self: ARuleVisualizer with BoolExpressionVisualizer =>

  private val arulesToPMMLMapper: PartialFunction[ARule, Map[String, Any]] = {
    case ar @ ARule(ant, con, im, ct) => Map(
      "id" -> s"AR${ar.hashCode}",
      "id-consequent" -> baref(con),
      "text" -> aruleToString(ar),
      "a" -> ct.a,
      "b" -> ct.b,
      "c" -> ct.c,
      "d" -> ct.d
    ) ++ ant.map(x => "id-antecedent" -> baref(x)).toMap
  }

  private val dbaToPMMLMapper = {
    def makeDbaMap(expr: BoolExpression[FixedValue], children: List[String]) = Map(
      "id" -> baref(expr),
      "text" -> exprToString(expr),
      "barefs" -> children
    )
    val pf: PartialFunction[BoolExpression[FixedValue], Map[String, Any]] = {
      case e @ ANDOR(x, y) => makeDbaMap(e, List(baref(x), baref(y)))
      case e @ NOT(x) => makeDbaMap(e, List(baref(x)))
    }
    pf
  }

  private val bbaToPMMLMapper: PartialFunction[BoolExpression[FixedValue], Map[String, Any]] = {
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

  private def collectExpression(be: BoolExpression[FixedValue]): Set[BoolExpression[FixedValue]] = be match {
    case ANDOR(x, y) => (collectExpression(x) ++ collectExpression(y)) + be
    case NOT(x) => collectExpression(x) + be
    case _ => Set(be)
  }

  def toPMML = {
    val exprs = arules.flatMap(x => x.consequent :: x.antecedent.toList).map(collectExpression).reduceOption(_ ++ _)
    Template.apply(
      "PMMLResult.template.mustache",
      Map(
        "arules" -> arules.collect(arulesToPMMLMapper),
        "dbas" -> exprs.getOrElse(Nil).collect(dbaToPMMLMapper),
        "bbas" -> exprs.getOrElse(Nil).collect(bbaToPMMLMapper),
        "number-of-rules" -> arules.size
      )
    ).trim
  }

}
