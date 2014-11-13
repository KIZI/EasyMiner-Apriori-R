package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.BadInputData
import cz.vse.easyminer.miner.MinerProcess
import cz.vse.easyminer.miner.MinerResult
import cz.vse.easyminer.miner.MinerTask
import org.rosuda.REngine.Rserve.RConnection

class MySQLAprioriRProcess(pmml: scala.xml.Elem, rServer: String, rPort: Int = 6311) extends MinerProcess {
  
  private val (dbServer, dbName, dbUser, dbPass, dbTableName) = {
    val extensions = (pmml \ "Header" \ "Extension").map(ext =>
      (ext \ "@name").text -> (ext \ "@value").text
    ).toMap
    List("database-server", "database-name", "database-user", "database-password", "dataset", "database-type").map(x => extensions.getOrElse(x, "")).filter(!_.isEmpty) match {
      case a @ List(dbServer, dbName, dbUser, dbPass, dbTableName, "mysql") => (dbServer, dbName, dbUser, dbPass, dbTableName)
      case x => throw new BadInputData(s"Missing data to prepare dataset. Given: $x")
    }
  }
  
  private val conn = new RConnection(rServer, rPort)
  
  def prepareDataset[T]: (MySQLDataset => T) => T = {
    MySQLDataset.apply(dbServer, dbName, dbUser, dbPass, dbTableName) _
  }
  
  def mine(mt: MinerTask) : MinerResult = {
    
  }
  
}
