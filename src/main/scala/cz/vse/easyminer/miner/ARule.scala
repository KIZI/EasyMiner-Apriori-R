package cz.vse.easyminer.miner

case class ARule(
  antecedent: BoolExpression[FixedValue],
  consequent: BoolExpression[FixedValue],
  interestMeasures: Set[InterestMeasure],
  contingencyTable: ContingencyTable
)

