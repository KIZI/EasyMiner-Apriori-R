package cz.vse.easyminer.rest

import spray.routing.Directives
import spray.routing.ExceptionHandler
import spray.routing.RejectionHandler
import spray.routing.RequestContext

trait DefaulHandlers extends Directives {
  val exceptionHandler : ExceptionHandler
  val rejectionHandler : RejectionHandler
  def handleDefault(body: RequestContext => Unit) = handleExceptions(exceptionHandler) {
    handleRejections(rejectionHandler)(body)
  }
}
