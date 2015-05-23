package cz.vse.easyminer.miner

trait RScript {

  val rcp: RConnectionPool

  private def normalizeScript(rscript: String) = rscript.trim.replaceAll("\r\n", "\n")

  def eval(rscript: String) = {
    val conn = rcp.borrow
    try {
      conn.parseAndEval(normalizeScript(rscript)).asStrings
    } finally {
      rcp.release(conn)
    }
  }

}

