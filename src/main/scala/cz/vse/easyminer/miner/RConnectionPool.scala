package cz.vse.easyminer.miner

import org.rosuda.REngine.Rserve.RConnection

trait RConnectionPool {
  def borrow : BorrowedConnection
  def release(bc: BorrowedConnection)
  def refresh()
  def close()
}

class BorrowedConnection(rServer : String, rPort : Int) extends RConnection(rServer, rPort) {
  val created = System.currentTimeMillis
}