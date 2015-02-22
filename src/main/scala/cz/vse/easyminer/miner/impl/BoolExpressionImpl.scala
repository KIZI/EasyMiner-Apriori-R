package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.AND
import cz.vse.easyminer.miner.AllValues
import cz.vse.easyminer.miner.Attribute
import cz.vse.easyminer.miner.BoolExpression
import cz.vse.easyminer.miner.BoolExpressionVisualizer
import cz.vse.easyminer.miner.FixedValue
import cz.vse.easyminer.miner.NOT
import cz.vse.easyminer.miner.OR
import cz.vse.easyminer.miner.Value

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
