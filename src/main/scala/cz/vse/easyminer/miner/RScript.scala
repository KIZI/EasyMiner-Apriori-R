package cz.vse.easyminer.miner

import org.rosuda.REngine.Rserve.RConnection
import cz.vse.easyminer.util.BasicFunction._

trait RScript {

  val rServer : String
  val rPort : Int
  
  private def normalizeScript(rscript: String) = rscript.trim.replaceAll("\r\n", "\n")
  
  def eval(rscript: String) = tryCloseBool(new RConnection(rServer, rPort))(_.parseAndEval(normalizeScript(rscript)).asStrings)
  
}
