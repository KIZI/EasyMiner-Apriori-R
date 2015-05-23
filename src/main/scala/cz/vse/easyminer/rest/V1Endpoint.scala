package cz.vse.easyminer.rest

import java.util.{Date, UUID}

import akka.actor.{ActorIdentity, ActorRefFactory, Identify}
import akka.pattern.ask
import akka.util.Timeout
import cz.vse.easyminer.util.RestUtils.PathExtension
import cz.vse.easyminer.util.Template
import spray.http.HttpHeaders.RawHeader
import spray.http.MediaTypes._
import spray.http.{ContentType, HttpCharsets, HttpEntity, StatusCodes}
import spray.routing.Directives
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success
import scala.xml.NodeSeq

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
                <task-id>{ id }</task-id>
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
      case Success(ActorIdentity(_, Some(minerActor))) =>
        import MinerControllerActor._
        val resReq = minerActor ? Sent.ResultRequest
        Await.result(resReq, 30 seconds) match {
          case Reply.Result(pmml) => complete(HttpEntity.apply(ContentType(`application/xml`, HttpCharsets.`UTF-8`), pmml))
          case Reply.Error(th) => throw th
          case _ => complete(
            StatusCodes.Accepted,
            <status>
              <code>202 Accepted</code>
              <miner>
                <state>In progress</state>
                <task-id>{ id }</task-id>
              </miner>
            </status>
          )
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
    } ~ attachDoc {
      uri =>
        val host = uri.authority.host + (if (uri.authority.port == 0) "" else ":" + uri.authority.port.toString)
        Template.apply("swagger-doc.json.mustache", Map("host" -> host))
    }
  }

}
