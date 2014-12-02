package cz.vse.easyminer.rest

import scala.xml.NodeSeq
import spray.routing.Directives

class V1Endpoint extends Directives {
  
  self: DefaulHandlers =>
  
  def apply = handleDefault {
    path("mine" ~ Slash.?) {
      post {
        requestEntityPresent {
          entity(as[NodeSeq]) {
            pmml => complete(pmml)
          }
        }
      }
    }
  }
  
}
