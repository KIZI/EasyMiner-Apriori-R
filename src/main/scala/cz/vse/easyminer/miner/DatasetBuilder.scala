package cz.vse.easyminer.miner

trait DatasetBuilder {
  def buildAndExecute[T](dbServer: String, dbName: String, dbUser: String, dbPass: String, dbTableName: String)(dbq: Dataset => T) : T
}