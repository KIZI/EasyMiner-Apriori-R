package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.BorrowedConnection
import cz.vse.easyminer.miner.RConnectionPool
import cz.vse.easyminer.util.Conf
import java.util.Date
import org.slf4j.LoggerFactory
import scala.concurrent._

class RConnectionPoolImpl(rServer: String, rPort: Int, prepareLibs: Boolean = true) extends RConnectionPool {

  val maxIdle = 10
  val minIdle = 2
  val connectionTimeout = 120
  val logger = LoggerFactory.getLogger("cz.vse.easyminer.miner.impl.RConnectionPool")

  private val pool = new collection.mutable.PriorityQueue[BorrowedConnection]()(new Ordering[BorrowedConnection] {
    def compare(a: BorrowedConnection, b: BorrowedConnection) = b.created compare a.created
  })
  private var activeConnections = 0

  private def createConnection = {
    val conn = new BorrowedConnection(rServer, rPort)
    if (prepareLibs) {
      conn.eval("library(RJDBC)")
      conn.eval("library(arules)")
    }
    logger.debug("New R connection has been created and prepared.")
    conn
  }

  def numActive = activeConnections
  def numIdle = pool.size

  def borrow = pool.synchronized {
    import ExecutionContext.Implicits.global
    activeConnections = activeConnections + 1
    val conn = try {
      pool.dequeue
    } catch {
      case _: NoSuchElementException => createConnection
    }
    future {
      refresh
    }
    logger.debug("R connection has been borrowed. Connection creation time: " + new Date(conn.created))
    conn
  }

  def release(bc: BorrowedConnection) = {
    import ExecutionContext.Implicits.global
    future {
      pool.synchronized {
        activeConnections = activeConnections - 1
        if (pool.size < maxIdle) {
          pool.enqueue(bc)
        } else {
          bc.close
        }
      }
    }
    logger.debug("R connection has been released.")
  }

  def refresh = pool.synchronized {
    while (pool.headOption.exists(_.created < System.currentTimeMillis - connectionTimeout * 1000)) {
      pool.dequeue.close
    }
    while ( /*pool.size < maxIdle && activeConnections > pool.size || */ pool.size < minIdle) {
      pool.enqueue(createConnection)
    }
    logger.debug(s"R connection pool has been refreshed. Current state is: active = $numActive, idle = $numIdle")
  }

  def close = pool.synchronized {
    pool.dequeueAll foreach (_.close)
  }

}

object RConnectionPoolImpl {

  val default = new RConnectionPoolImpl(Conf().get[String]("r-miner.rserve-address"), Conf().getOrElse[Int]("r-miner.rserve-port", 6311))

}