package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.BorrowedConnection
import cz.vse.easyminer.miner.RConnectionPool
import cz.vse.easyminer.util.Conf

class RConnectionPoolImpl(rServer : String, rPort : Int) extends RConnectionPool {

  val maxIdle = 10
  val minIdle = 2
  val connectionTimeout = 120
  private val pool = new collection.mutable.PriorityQueue[BorrowedConnection]()(new Ordering[BorrowedConnection] {
      def compare(a:BorrowedConnection, b:BorrowedConnection) = b.created compare a.created
    }
  )
  private var activeConnections = 0 
  
  private def createConnection = {
    val conn = new BorrowedConnection(rServer, rPort)
    conn.eval("library(RJDBC)")
    conn.eval("library(arules)")
    conn
  }
  
  def numActive = activeConnections
  def numIdle = pool.size
  
  def borrow = pool.synchronized {
    activeConnections = activeConnections + 1
    try {
      pool.dequeue
    } catch {
      case _: NoSuchElementException => createConnection
    }
  }
  
  def release(bc: BorrowedConnection) = pool.synchronized {
    activeConnections = activeConnections - 1
    if (pool.size < maxIdle) {
      pool.enqueue(bc)
    } else {
      bc.close
    }
  }
  
  def refresh = pool.synchronized {
    while(pool.headOption.exists(_.created < System.currentTimeMillis - connectionTimeout)) {
      pool.dequeue.close
    }
    while (/*pool.size < maxIdle && activeConnections > pool.size || */pool.size < minIdle) {
      pool.enqueue(createConnection)
    }
  }
  
}

object RConnectionPoolImpl {
  
  val default = new RConnectionPoolImpl(Conf().get[String]("r-miner.rserve-address"), Conf().getOrElse[Int]("r-miner.rserve-port", 6311))
  default.refresh
  
}