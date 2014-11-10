package cz.vse.easyminer.miner

case class MinerTask(
  antecedent : BoolExpression[Attribute],
  interestMeasures : Set[InterestMeasure],
  consequent : BoolExpression[Attribute]
)

