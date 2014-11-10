package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.MinerProcess
import cz.vse.easyminer.miner.MinerTask
import org.rosuda.REngine.Rserve.RConnection

abstract class RAprioriProcess(
  dbServer: String,
  dbName: String,
  dbUser: String,
  dbPass: String,
  dbTableName: String,
  rServer: String,
  rPort: Int = 6311
)  {
  
  private val conn = new RConnection(rServer, rPort)
  
  
}
