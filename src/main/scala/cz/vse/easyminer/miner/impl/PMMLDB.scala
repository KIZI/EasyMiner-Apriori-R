package cz.vse.easyminer.miner.impl

trait PMMLDB {

  val pmml: xml.Node
  val dbServer : String
  val dbName : String
  val dbUser : String
  val dbPass : String
  val dbTableName : String
  
}
