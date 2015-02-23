package cz.vse.easyminer.miner

sealed trait InterestMeasure

case class Confidence(value: Double) extends InterestMeasure
case class Support(value: Double) extends InterestMeasure
case class Lift(value: Double) extends InterestMeasure
case class Count(value: Int) extends InterestMeasure
case class Limit(value: Int) extends InterestMeasure