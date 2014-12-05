package cz.vse.easyminer.util

import spray.http.Uri

object RestUtils {
  
  class PathExtension(path: Uri.Path) {
    private def findClosestParent(path: Uri.Path) : Uri.Path = path match {
      case Uri.Path.Empty => path
      case Uri.Path.Slash(tail) => findClosestParent(tail)
      case Uri.Path.Segment(_, Uri.Path.Slash(tail)) => tail
      case Uri.Path.Segment(_, path @ Uri.Path.Empty) => path 
    }
    def parent = findClosestParent(path.reverse).reverse
  }
  
  object PathExtension {
    import scala.language.implicitConversions
    implicit def pathToPathExtension(path: Uri.Path) = new PathExtension(path)
  }
  
}