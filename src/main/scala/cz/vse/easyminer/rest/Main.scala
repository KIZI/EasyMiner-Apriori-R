package cz.vse.easyminer.rest

import akka.actor.ActorSystem
import cz.vse.easyminer.util.Conf
import spray.routing.SimpleRoutingApp

object Main extends App with SimpleRoutingApp {

  implicit val system = ActorSystem("easyminer-rest-system")

  val v1Endpoint = new V1Endpoint with DefaultXmlHandlers with EndpointDoc

  startServer(interface = Conf().get[String]("rest.address"), port = Conf().get[Int]("rest.port")) {
    pathPrefix("api") {
      pathPrefix("v1") {
        v1Endpoint.endpoint
      }
    }
  }

}