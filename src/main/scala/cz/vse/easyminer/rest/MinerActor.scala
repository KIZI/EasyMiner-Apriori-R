package cz.vse.easyminer.rest

import akka.actor.Actor
import org.slf4j.LoggerFactory
import scala.xml.NodeSeq

class MinerActor extends Actor {

  val logger = LoggerFactory.getLogger("MinerActor")
  val path = self.path.toStringWithoutAddress
  
  def receive = {
    case pmml: NodeSeq => {
        logger.debug(s"$path: mining starting...")
        Thread.sleep(60000)
        //tady bude probihat samotne minovani
      }
  }
  
  override def postStop = {
    logger.debug(s"$path: stopping...")
  }
  
}