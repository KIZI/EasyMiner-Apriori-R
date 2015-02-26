import cz.vse.easyminer.miner.AllValues
import cz.vse.easyminer.miner.Confidence
import cz.vse.easyminer.miner.FixedValue
import cz.vse.easyminer.miner.Lift
import cz.vse.easyminer.miner.Limit
import cz.vse.easyminer.miner.Support
import cz.vse.easyminer.miner.Value
import cz.vse.easyminer.miner.impl.PMMLTask
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class InputSpec extends FlatSpec with Matchers with TemplateOpt {

  lazy val task1 = new PMMLTask(inputpmml2.get)
  lazy val task2 = new PMMLTask(inputpmml3.get)

  "PMML Task 1" should "have antecedent and consequent" in {
    task1.fetchAntecedent should be (Value(AllValues("District'")))
    task1.fetchConsequent should be (Value(FixedValue("status_s_diakritikou","Cý\"")))
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
