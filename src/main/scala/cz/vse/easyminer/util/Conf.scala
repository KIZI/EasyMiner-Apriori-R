package cz.vse.easyminer.util

import com.github.kxbmap.configs._
import com.typesafe.config.ConfigFactory

object Conf {

  private val config = new Conf(new EnrichTypesafeConfig(ConfigFactory.load))

  def apply() = config
  
}

class Conf(etc: EnrichTypesafeConfig) {
  
  def get[T: AtPath](path: String): T = etc.get(path)

  def opt[T: AtPath](path: String)(implicit cc: CatchCond = CatchCond.configException): Option[T] = etc.opt(path)

  def getOrElse[T: AtPath](path: String, default: => T)(implicit cc: CatchCond = CatchCond.missing): T = etc.getOrElse(path, default)
  
}
