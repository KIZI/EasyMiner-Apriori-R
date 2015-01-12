package cz.vse.easyminer.rest

import akka.actor.ActorIdentity
import akka.actor.ActorRefFactory
import akka.util.Timeout
import cz.vse.easyminer.util.RestUtils.PathExtension
import cz.vse.easyminer.util.Template
import java.util.Date
import java.util.UUID
import scala.util.Success
import scala.xml.NodeSeq
import spray.http.ContentType
import spray.http.HttpEntity
import spray.http.HttpHeaders
import spray.http.HttpHeaders.RawHeader
import spray.http.MediaTypes
import spray.http.StatusCodes
import spray.routing.Directives
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.Identify
import akka.pattern.ask
import scala.language.postfixOps
import MediaTypes._

class V1Endpoint(implicit af: ActorRefFactory) extends Directives {

  self: DefaulHandlers with EndpointDoc =>

  private implicit val timeout = Timeout(10 seconds)
  private implicit val dispatcher = af.dispatcher

  private def idMinerPrefix = "miner-"

  private def sendPmml(pmml: NodeSeq) = {
    import MinerControllerActor._
    val id = UUID.randomUUID.toString
    val minerActor = af.actorOf(MinerControllerActor.props(idMinerPrefix + id), idMinerPrefix + id)
    minerActor ! Sent.Task(pmml)
    requestUri { uri =>
      {
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

  private def receiveResult(id: UUID) = {
    val actorExists = af.actorSelection("/user/" + idMinerPrefix + id.toString) ? Identify(1)
    Await.ready(actorExists, 10 seconds).value.get match {
      case Success(ActorIdentity(_, Some(minerActor))) => {
        import MinerControllerActor._
        val resReq = minerActor ? Sent.ResultRequest
        Await.result(resReq, 30 seconds) match {
          case Reply.Result(pmml) => complete(HttpEntity.apply(ContentType(`application/xml`), pmml))
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

  def endpoint = handleDefault {
    path("mine" ~ Slash.?) {
      post {
        requestEntityPresent {
          entity(as[NodeSeq]) {
            sendPmml
          }
        }
      }
    } ~ path("result" / JavaUUID ~ Slash.?) {
      id =>
        get {
          receiveResult(id)
        }
    } ~ attachDoc(uri => Template.apply("swagger-doc.json.mustache", Map("host" -> s"${uri.authority.host}:${uri.authority.port}")))
  }

}
