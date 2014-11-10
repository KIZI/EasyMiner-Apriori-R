package cz.vse.easyminer.miner

sealed trait Attribute
case class *(name: String) extends Attribute
case class FixedValue(name: String, value: String) extends Attribute