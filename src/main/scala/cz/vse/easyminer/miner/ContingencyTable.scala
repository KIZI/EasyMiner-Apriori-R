package cz.vse.easyminer.miner

case class ContingencyTable(a: Int, b: Int, c: Int, d: Int)

object ContingencyTable {
  
  def apply(supp: Support, conf: Confidence, lift: Lift, count: Count) = {
    val a = math.rint(supp.value * count.value).toInt
    val b = math.rint(a / conf.value - a).toInt
    val c = math.rint((conf.value * count.value) / lift.value - a).toInt
    val d = count.value - a - b - c
    new ContingencyTable(a, b, c, d)
  }
  
}