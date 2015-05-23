package cz.vse.easyminer.rest

import org.slf4j.LoggerFactory
import spray.http.ContentType
import spray.http.HttpData
import spray.http.HttpEntity
import spray.routing.ExceptionHandler
import spray.routing.RejectionHandler

trait DefaultXmlHandlers extends DefaulHandlers {

  def getErrorMessage(code: Int, name: String, msg: String) =
    <error>
      <code>{ code }</code>
      <name>{ name }</name>
      <message>{ msg }</message>
    </error>
  
  val exceptionHandler = ExceptionHandler {
    case e: Throwable =>
      requestUri { uri =>
        LoggerFactory.getLogger(e.getClass.getName).error(s"Error with URI $uri", e)
        complete(500, getErrorMessage(500, e.getClass.getName, e.getMessage))
      }
  }
  
  val rejectionHandler = {
    import spray.http.MediaTypes._
    import spray.http.HttpCharsets._
    RejectionHandler {
      case rejections => mapHttpResponse(x =>
          x.withEntity(
            HttpEntity(
              ContentType(`application/xml`, `UTF-8`), HttpData(getErrorMessage(x.status.intValue, x.status.reason, x.entity.asString).toString())
            )
          )
        ) {
          RejectionHandler.Default(rejections)
        }
    }
  }
  
}
