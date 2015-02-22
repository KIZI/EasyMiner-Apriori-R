package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.AllValues
import cz.vse.easyminer.miner.Attribute
import cz.vse.easyminer.miner.AttributeValueNormalizer
import cz.vse.easyminer.miner.FixedValue

trait AttributeValueNormalizerImpl extends AttributeValueNormalizer {

  import scala.language.implicitConversions
  
  implicit def AttributeToNormalizedAttribute(attr: Attribute) = new NormalizedAttributeImpl(attr)

  class NormalizedAttributeImpl(attr: Attribute) extends NormalizedAttribute {
    private def normalizeText(str: String) = str.replaceAll("\"|'", "")
    def normalize = attr match {
      case x @ AllValues(name) => x.copy(name = normalizeText(name))
      case x @ FixedValue(name, value) => x.copy(name = normalizeText(name), value = normalizeText(value))
      case _ => attr
    }
  }

}
