import cz.vse.easyminer.miner.BadInputData
import cz.vse.easyminer.miner.Confidence
import cz.vse.easyminer.miner.Lift
import cz.vse.easyminer.miner.Limit
import cz.vse.easyminer.miner.Support
import cz.vse.easyminer.miner.impl.AttributeValidatorImpl
import cz.vse.easyminer.miner.impl.PMMLTask
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class InputSpec extends FlatSpec with Matchers with TemplateOpt {

  lazy val task1 = new PMMLTask(inputpmml2.get) with AttributeValidatorImpl
  lazy val task2 = new PMMLTask(inputpmml3.get) with AttributeValidatorImpl

  "PMML Task 1" should "throw BadInputData for quotation marks" in {
    intercept[BadInputData] {
      task1.fetchAntecedent
    }
    intercept[BadInputData] {
      task1.fetchConsequent
    }
  }

  it should "have all interest measures" in {
    val im = task1.fetchInterestMeasures
    im should contain(Lift(1.3))
    im should contain(Confidence(0.1))
    im should contain(Support(0.01))
    im should contain(Limit(100))
  }

  "PMML Task 2" should "not have limit and lift" in {
    task2.fetchInterestMeasures exists {
      case Lift(_) | Limit(_) => true
      case _ => false
    } should be(false)
  }

}
