package cz.vse.easyminer.miner

sealed trait BoolExpression[+T] {
  def AND[A >: T](expr: BoolExpression[A]) : BoolExpression[A] = new AND(this, expr)
  def OR[A >: T](expr: BoolExpression[A]): BoolExpression[A] = new OR(this, expr)
  def NOT: BoolExpression[T] = new NOT(this)
}

case class Value[T](x: T) extends BoolExpression[T]
case class AND[T](a: BoolExpression[T], b: BoolExpression[T]) extends BoolExpression[T]
case class OR[T](a: BoolExpression[T], b: BoolExpression[T]) extends BoolExpression[T]
case class NOT[T](a: BoolExpression[T]) extends BoolExpression[T]