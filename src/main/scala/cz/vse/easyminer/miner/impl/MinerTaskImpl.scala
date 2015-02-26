package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.ANDOR
import cz.vse.easyminer.miner.AllValues
import cz.vse.easyminer.miner.Attribute
import cz.vse.easyminer.miner.BadInputData
import cz.vse.easyminer.miner.BoolExpression
import cz.vse.easyminer.miner.Confidence
import cz.vse.easyminer.miner.FixedValue
import cz.vse.easyminer.miner.Limit
import cz.vse.easyminer.miner.MinerTask
import cz.vse.easyminer.miner.MinerTaskValidator
import cz.vse.easyminer.miner.NOT
import cz.vse.easyminer.miner.Support
import cz.vse.easyminer.miner.Value

trait MinerTaskValidatorImpl extends MinerTaskValidator {
  private def validateText(str: String) = str.foreach {
    case '"' | '\'' => throw new BadInputData("Attribute can not contain any quotation mark.")
    case _ =>
  }
  private def validateValue(x: Attribute) = x match {
    case AllValues(name) => validateText(name)
    case FixedValue(name, value) => {
      validateText(name)
      validateText(value)
    }
    case _ =>
  }
  private def validateBoolExp(exp: BoolExpression[Attribute]): Unit = exp match {
    case ANDOR(a, b) => {
      validateBoolExp(a)
      validateBoolExp(b)
    }
    case NOT(a) => validateBoolExp(a)
    case Value(x) => validateValue(x)
  }
  def validate(mt: MinerTask) = {
    validateBoolExp(mt.antecedent)
    validateBoolExp(mt.consequent)
    if (mt.interestMeasures.count {
      case Confidence(_) | Support(_) => true
      case _ => false
    } < 2) throw new BadInputData("Confidence and Support are required.")
    for (x <- mt.interestMeasures) x match {
      case Confidence(x) if x > 1 || x < 0.001 => throw new BadInputData("Confidence must be greater than 0.001 and less than 1.")
      case Support(x) if x > 1 || x < 0.001 => throw new BadInputData("Support must be greater than 0.001 and less than 1.")
      case Limit(x) if x <= 0 => throw new BadInputData("Limit must be greater than 0.")
      case _ =>
    }
  }
}