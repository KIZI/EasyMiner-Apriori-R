package cz.vse.easyminer.rest

import akka.actor.Actor
import akka.actor.FSM
import akka.actor.Props
import scala.xml.NodeSeq
import akka.actor.ReceiveTimeout
import org.slf4j.LoggerFactory
import scala.concurrent.duration._
import scala.language.postfixOps

class MinerControllerActor(id: String) extends Actor with FSM[MinerControllerActor.State, MinerControllerActor.Data] {
  
  import MinerControllerActor._
  
  val child = context.actorOf(Props[MinerActor], name = "miner")
  val logger = LoggerFactory.getLogger("cz.vse.easyminer.rest.MinerControllerActor")
  
  setTimer("timeout", ReceiveTimeout, 2 minutes, false)
  
  startWith(State.Waiting, Data.NoData)
  
  when(State.Waiting) {
    case Event(Sent.Task(pmml), _) => {
        logger.debug(s"$id: Sent.Task(pmml) -> State.InProgress")
        child ! pmml
        goto(State.InProgress)
      }
  }
  
  when(State.InProgress) {
    case Event(Sent.ResultRequest, _) => {
        logger.debug(s"$id: Sent.ResultRequest -> Reply.InProgress")
        stay replying Reply.InProgress
      }
    case Event(Sent.Error(th), _) => {
        logger.debug(s"$id: Sent.Error -> State.Finished, Data.Error($th)")
        goto(State.Finished) using Data.Error(th)
      }
    case Event(Sent.Result(pmml), _) => {
        logger.debug(s"$id: Sent.Result -> State.Finished, Data.Result(pmml)")
        goto(State.Finished) using Data.Result(pmml)
      }
  }
  
  when(State.Finished) {
    case Event(Sent.ResultRequest, Data.Result(pmml)) => {
        logger.debug(s"$id: Sent.ResultRequest -> Reply.Result(pmml), stop")
        stop replying Reply.Result(pmml) 
      }
    case Event(Sent.ResultRequest, Data.Error(th)) => {
        logger.debug(s"$id: Sent.ResultRequest -> Reply.Error($th), stop")
        stop replying Reply.Error(th) 
      }
  }
  
  whenUnhandled {
    case Event(ReceiveTimeout, _) => {
        logger.debug(s"$id: Sent.ReceiveTimeout -> stop")
        stop
      }
    case Event(_, _) => {
        logger.warn(s"$id: Sent.Undefined -> stop")
        stop
      }
  }
  
}

object MinerControllerActor {
  
  def props(id: String): Props = Props(new MinerControllerActor(id))
  
  sealed trait Reply
  object Reply {
    object InProgress extends Reply
    case class Result(pmml: String) extends Reply
    case class Error(th: Throwable) extends Reply
  }
  
  sealed trait Sent
  object Sent {
    case class Task(pmml: NodeSeq) extends Sent
    case class Result(pmml: String) extends Sent
    case class Error(th: Throwable) extends Sent
    object ResultRequest extends Sent
  }
  
  sealed trait State
  object State {
    object Waiting extends State
    object InProgress extends State
    object Finished extends State
  }
  
  sealed trait Data
  object Data {
    object NoData extends Data
    case class Error(ex: Throwable) extends Data
    case class Result(pmml: String) extends Data
  }
  
}