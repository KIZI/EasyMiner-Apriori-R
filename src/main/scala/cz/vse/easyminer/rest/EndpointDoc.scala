package cz.vse.easyminer.rest

import akka.actor.ActorRefFactory
import spray.http.ContentType
import spray.http.HttpEntity
import spray.http.MediaTypes
import spray.http.StatusCodes
import spray.http.Uri
import spray.routing.Directives
import MediaTypes._
import spray.routing.RoutingSettings
import spray.routing.directives.ContentTypeResolver

trait EndpointDoc extends Directives {

  def attachDoc(swaggerJson: Uri => String)(implicit settings: RoutingSettings, resolver: ContentTypeResolver, refFactory: ActorRefFactory) = pathSingleSlash {
    getFromFile("webapp/index.html")
  } ~ pathEnd {
    requestUri { uri =>
      redirect(uri.withPath(uri.path / ""), StatusCodes.PermanentRedirect)
    }
  } ~ path("swagger-doc.json") {
    requestUri { uri =>
      complete(HttpEntity.apply(ContentType(`application/json`), swaggerJson(uri)))
    }
  } ~ getFromDirectory("webapp")

}
