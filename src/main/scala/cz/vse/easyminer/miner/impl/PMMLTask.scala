package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.BadInputData
import cz.vse.easyminer.miner.MinerTask
import java.util.UUID
import scala.collection.immutable.Queue
import scalikejdbc._

object PMMLMySQLTask {
  


  
}

class PMMLMySQLTask(pmml: scala.xml.Elem) {
  
  def prepareDataset[T]: (MySQLDataset => T) => T = {
    val extensions = (pmml \ "Header" \ "Extension").map(ext =>
      (ext \ "@name").text -> (ext \ "@value").text
    ).toMap
    List("mysql-server", "mysql-database", "mysql-user", "mysql-password", "dataset").map(x => extensions.getOrElse(x, "")).filter(!_.isEmpty) match {
      case a @ List(dbServer, dbName, dbUser, dbPass, dbTableName) => MySQLDataset.apply(dbServer, dbName, dbUser, dbPass, dbTableName) _
      case x => throw new BadInputData(s"Missing data to prepare dataset. Given: $x")
    }
  }
  
  def fetchAntecedent(implicit dataset: MySQLDataset) = {
    
  }
    
}
