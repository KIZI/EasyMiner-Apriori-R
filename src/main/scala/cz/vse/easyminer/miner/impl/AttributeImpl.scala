package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.AllValues
import cz.vse.easyminer.miner.Attribute
import cz.vse.easyminer.miner.AttributeValidator
import cz.vse.easyminer.miner.BadInputData
import cz.vse.easyminer.miner.FixedValue

trait AttributeValidatorImpl extends AttributeValidator {
  private def textValidate(str: String) = str.foreach {
    case '"' | '\'' => throw new BadInputData("Attribute can not contain any quotation mark.")
    case _ =>
  }
  def validate(attr: Attribute) = attr match {
    case x @ AllValues(name) => textValidate(name)
    case x @ FixedValue(name, value) => {
      textValidate(name)
      textValidate(value)
    }
    case _ =>
  }
}