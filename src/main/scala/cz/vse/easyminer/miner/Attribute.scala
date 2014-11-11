package cz.vse.easyminer.miner

sealed trait Attribute
object * extends Attribute
case class AllValues(name: String) extends Attribute
case class FixedValue(name: String, value: String) extends Attribute