package cz.vse.easyminer.util

import org.fusesource.scalate.TemplateEngine

object Template {
  
  val defaultBasePath: String = "/cz/vse/easyminer/"
  
  def apply(name: String, attributes: Map[String, Any] = Map.empty)(implicit basePath: String = defaultBasePath): String = (new TemplateEngine).layout(basePath + name, attributes)
  
}