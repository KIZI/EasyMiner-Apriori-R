package cz.vse.easyminer.rest

import akka.actor.Actor
import cz.vse.easyminer.miner.{BadInputData, MinerTask}
import cz.vse.easyminer.miner.impl.{ARuleText, AprioriRProcess, BoolExpressionShortText, DBOptsPMML, MinerTaskValidatorImpl, MySQLDatasetBuilder, MySQLQueryBuilder, PMMLResult, PMMLTask, RConnectionPoolImpl}
import cz.vse.easyminer.rest.MinerControllerActor._
import cz.vse.easyminer.util.Conf
import org.slf4j.LoggerFactory
import scala.xml.NodeSeq

class MinerActor extends Actor {

  val logger = LoggerFactory.getLogger("cz.vse.easyminer.rest.MinerActor")
  val path = self.path.toStringWithoutAddress

  def receive = {
    case pmml: NodeSeq =>
      val starttime = System.currentTimeMillis
      logger.info(s"$path: mining start...")
      try {
        pmml.find(_.label == "PMML") match {
          case Some(_pmml) =>
            logger.trace("PMML Input:\n" + _pmml)
            val task = new PMMLTask(_pmml)
            val process = new AprioriRProcess(
              "RAprioriWithMySQL.mustache",
              Conf().get[String]("r-miner.jdbc-driver-dir-absolute-path"),
              RConnectionPoolImpl.default
            ) with MinerTaskValidatorImpl with MySQLDatasetBuilder with MySQLQueryBuilder with DBOptsPMML {
              val pmml = _pmml
            }
            val minertask = MinerTask(task.fetchAntecedent, task.fetchInterestMeasures, task.fetchConsequent)
            val result = process.mine(minertask)
            val pmmlresult = (new PMMLResult(result) with ARuleText with BoolExpressionShortText).toPMML
            logger.trace("PMML Output:\n" + pmmlresult)
            logger.info(s"$path: mining end in ${System.currentTimeMillis - starttime}ms")
            sender ! Sent.Result(pmmlresult)
          case None => throw new BadInputData("PMML element not found!")
        }
      } catch {
        case th: Throwable => sender ! Sent.Error(th)
      }
  }

  override def postStop() = {
    logger.debug(s"$path: stopping...")
  }

}