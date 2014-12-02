package cz.vse.easyminer.miner

trait DatasetQueryBuilder {
  def toSQLSelect(exp: BoolExpression[Attribute]) : Traversable[String]
  def toSQLSelectMap(exp: BoolExpression[Attribute]) : Map[String, String]
}
