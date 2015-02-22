package cz.vse.easyminer.miner

sealed trait Attribute
object * extends Attribute
case class AllValues(name: String) extends Attribute
case class FixedValue(name: String, value: String) extends Attribute

trait AttributeValueNormalizer {

  import scala.language.implicitConversions
  
  implicit def AttributeToNormalizedAttribute(attr: Attribute): NormalizedAttribute

  trait NormalizedAttribute {
    def normalize: Attribute
  }

}