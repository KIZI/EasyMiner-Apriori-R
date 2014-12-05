package cz.vse.easyminer.rest

import akka.actor.ActorIdentity
import akka.actor.ActorRefFactory
import akka.util.Timeout
import cz.vse.easyminer.util.RestUtils
import java.util.Date
import java.util.UUID
import scala.util.Failure
import scala.util.Success
import scala.xml.NodeSeq
import spray.http.HttpHeaders.RawHeader
import spray.http.StatusCodes
import spray.routing.Directives
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.Identify
import akka.pattern.ask
import scala.language.postfixOps

class V1Endpoint(implicit af: ActorRefFactory) extends Directives {
  
  self: DefaulHandlers =>
  
  implicit val timeout = Timeout(10 seconds)
  implicit val dispatcher = af.dispatcher
  
  def idMinerPrefix = "miner-"
  
  def sendPmml(pmml: NodeSeq) = {
    import MinerControllerActor._
    val id = UUID.randomUUID.toString
    val minerActor = af.actorOf(MinerControllerActor.props(idMinerPrefix + id), idMinerPrefix + id)
    minerActor ! Sent.Task(pmml)
    requestUri { uri => {
        import RestUtils.PathExtension._
        val rurl = uri.path.parent.toString + "/result/" + id
        respondWithHeader(RawHeader("Location", rurl)) {
          complete(
            StatusCodes.Accepted,
            <status>
              <code>202 Accepted</code>
              <miner>
                <state>In progress</state>
                <started>{ new Date }</started>
                <result-url>{ rurl }</result-url>
              </miner>
            </status>
          )
        }
      }
    }
  }
  
  def receiveResult(id: UUID) = {
    val actorExists = af.actorSelection("/user/" + idMinerPrefix + id.toString) ? Identify(1)
    Await.ready(actorExists, 10 seconds).value.get match {
      case Success(ActorIdentity(_, Some(minerActor))) => {
          import MinerControllerActor._
          val resReq = minerActor ? Sent.ResultRequest
          Await.result(resReq, 10 seconds) match {
            case Reply.Result(pmml) => complete(pmml)
            case Reply.Error(th) => throw th
            case _ => complete(
                StatusCodes.Accepted,
                <status>
                  <code>202 Accepted</code>
                  <miner>
                    <state>In progress</state>
                  </miner>
                </status>
              )
          }
        }
      case _ => reject
    }
  }
  
  def apply = handleDefault {
    path("mine" ~ Slash.?) {
      post {
        requestEntityPresent {
          entity(as[NodeSeq]) {
            sendPmml
          }
        }
      }
    } ~
    path("result" / JavaUUID ~ Slash.?) {
      id => get {
        receiveResult(id)
      } 
    }
  }
  
}
