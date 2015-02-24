package cz.vse.easyminer.rest

import akka.actor.Actor
import cz.vse.easyminer.miner.BadInputData
import cz.vse.easyminer.miner.MinerTask
import cz.vse.easyminer.miner.impl.ARuleText
import cz.vse.easyminer.miner.impl.AprioriRProcess
import cz.vse.easyminer.miner.impl.AttributeValidatorImpl
import cz.vse.easyminer.miner.impl.BoolExpressionText
import cz.vse.easyminer.miner.impl.DBOptsPMML
import cz.vse.easyminer.miner.impl.MySQLDatasetBuilder
import cz.vse.easyminer.miner.impl.MySQLQueryBuilder
import cz.vse.easyminer.miner.impl.PMMLResult
import cz.vse.easyminer.miner.impl.PMMLTask
import cz.vse.easyminer.util.Conf
import org.slf4j.LoggerFactory
import scala.xml.NodeSeq
import MinerControllerActor._

class MinerActor extends Actor {

  val logger = LoggerFactory.getLogger("cz.vse.easyminer.rest.MinerActor")
  val path = self.path.toStringWithoutAddress

  def receive = {
    case pmml: NodeSeq => {
      val starttime = System.currentTimeMillis
      logger.info(s"$path: mining start...")
      try {
        pmml.find(_.label == "PMML") match {
          case Some(_pmml) => {
            logger.trace("PMML Input:\n" + _pmml)
            val task = new PMMLTask(_pmml) with AttributeValidatorImpl
            val process = new AprioriRProcess(
              "RAprioriWithMySQL.mustache",
              Conf().get[String]("r-miner.jdbc-driver-dir-absolute-path"),
              Conf().get[String]("r-miner.rserve-address"),
              Conf().getOrElse[Int]("r-miner.rserve-port", 6311)
            ) with MySQLDatasetBuilder with MySQLQueryBuilder with DBOptsPMML {
              val pmml = _pmml
            }
            val minertask = MinerTask(task.fetchAntecedent, task.fetchInterestMeasures, task.fetchConsequent)
            val result = process.mine(minertask)
            val pmmlresult = (new PMMLResult(result) with ARuleText with BoolExpressionText).toPMML
            logger.trace("PMML Output:\n" + pmmlresult)
            logger.info(s"$path: mining end in ${System.currentTimeMillis - starttime}ms")
            sender ! Sent.Result(pmmlresult)
          }
          case None => throw new BadInputData("PMML element not found!")
        }
      } catch {
        case th: Throwable => sender ! Sent.Error(th)
      }
    }
  }

  override def postStop = {
    logger.debug(s"$path: stopping...")
  }

}