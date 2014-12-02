package cz.vse.easyminer.miner

case class ARule(
  antecedent: BoolExpression[FixedValue],
  consequent: BoolExpression[FixedValue],
  interestMeasures: Set[InterestMeasure],
  contingencyTable: ContingencyTable
)

trait ARuleVisualizer {
  def aruleToString(arule: ARule) : String
}