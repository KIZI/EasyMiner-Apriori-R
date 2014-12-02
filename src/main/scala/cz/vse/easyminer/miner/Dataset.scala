package cz.vse.easyminer.miner

trait Dataset {
  def fetchValuesBySelectAndColName(select: String, col: String) : List[Option[String]]
  def fetchValuesByColName(col: String) : List[String]
  def fetchCount : Int
}
