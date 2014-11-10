package cz.vse.easyminer.miner

sealed trait InterestMeasure

case class Confidence(value: Double) extends InterestMeasure
case class Support(value: Double) extends InterestMeasure
case class Lift(value: Double) extends InterestMeasure