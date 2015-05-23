package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner._

trait MinerTaskValidatorImpl extends MinerTaskValidator {

  private def validateText(str: String) = str.foreach {
    case '"' | '\'' => throw new BadInputData("Attribute can not contain any quotation mark.")
    case _ =>
  }

  private def validateValue(x: Attribute) = x match {
    case AllValues(name) => validateText(name)
    case FixedValue(name, value) =>
      validateText(name)
      validateText(value)
    case _ =>
  }

  private def validateBoolExp[A](exp: BoolExpression[Attribute])(implicit f: Attribute => A, join: (A, A) => A): A = exp match {
    case ANDOR(a, b) => join(validateBoolExp[A](a), validateBoolExp[A](b))
    case NOT(a) => validateBoolExp[A](a)
    case Value(x) => f(x)
  }

  def validate(mt: MinerTask) = {
    validateBoolExp[Unit](mt.antecedent)(validateValue, (_, _) => Unit)
    validateBoolExp[Unit](mt.consequent)(validateValue, (_, _) => Unit)
    if (mt.interestMeasures.count {
      case Confidence(_) | Support(_) => true
      case _ => false
    } < 2) throw new BadInputData("Confidence and Support are required.")
    mt.interestMeasures.foreach {
      case Confidence(v) if v > 1 || v < 0.001 => throw new BadInputData("Confidence must be greater than 0.001 and less than 1.")
      case Support(v) if v > 1 || v < 0.001 => throw new BadInputData("Support must be greater than 0.001 and less than 1.")
      case Limit(v) if v <= 0 => throw new BadInputData("Limit must be greater than 0.")
      case RuleLength(v) if v <= 0 => throw new BadInputData("Rule length must be greater than 0.")
      case _ =>
    }
    if (mt.interestMeasures.contains(CBA)) {
      val cbaException = new BadInputData("You may use only one attribute as the consequent if the CBA pruning is turned on.")
      val attributes = validateBoolExp[Set[String]](mt.consequent)(
      {
        case AllValues(name) => Set(name)
        case FixedValue(name, _) => Set(name)
        case _ => throw cbaException
      },
      _ ++ _
      )
      if (attributes.size > 1) throw cbaException
    }
  }

}