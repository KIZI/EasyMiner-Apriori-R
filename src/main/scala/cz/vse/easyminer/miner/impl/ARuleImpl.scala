package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.ARule
import cz.vse.easyminer.miner.ARuleVisualizer
import cz.vse.easyminer.miner.BoolExpressionVisualizer

trait ARuleText extends ARuleVisualizer {

  self: BoolExpressionVisualizer =>

  def aruleToString(arule: ARule): String = arule.antecedent.map(exprToString).getOrElse("*") + " &rarr; " + exprToString(arule.consequent)

}
