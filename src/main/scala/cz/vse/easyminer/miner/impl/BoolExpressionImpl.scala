package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.{AND, AllValues, BoolExpression, BoolExpressionVisualizer, FixedValue, NOT, OR, Value}

trait BoolExpressionText extends BoolExpressionVisualizer {

  def exprToString[Attribute](expr: BoolExpression[Attribute]) : String = expr match {
    case Value(AllValues(name)) => s"$name(*)"
    case Value(FixedValue(name, value)) => s"$name($value)"
    case AND(a, b) => "( " + exprToString(a) + " & " + exprToString(b) + " )"
    case OR(a, b) => "( " + exprToString(a) + " | " + exprToString(b) + " )"
    case NOT(a) => "^( " + exprToString(a) + " )"
    case _ => ""
  }
  
}

trait BoolExpressionShortText extends BoolExpressionVisualizer {

  def exprToString[Attribute](expr: BoolExpression[Attribute]) : String = expr match {
    case Value(AllValues(name)) => s"$name(*)"
    case Value(FixedValue(name, value)) => s"$name($value)"
    case AND(a, b) => exprToString(a) + " & " + exprToString(b)
    case OR(a, b) => exprToString(a) + " | " + exprToString(b)
    case NOT(a) => "^" + exprToString(a)
    case _ => ""
  }
  
}
