import cz.vse.easyminer.miner._
import cz.vse.easyminer.miner.impl.PMMLTask
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class InputSpec extends FlatSpec with Matchers with TemplateOpt {

  lazy val task1 = new PMMLTask(inputpmml2.get)
  lazy val task2 = new PMMLTask(inputpmml3.get)

  "PMML Task 1" should "have antecedent and consequent" in {
    task1.fetchAntecedent should be(Value(AllValues("District'")))
    task1.fetchConsequent should be(Value(FixedValue("status_s_diakritikou", "Cý\"")))
  }

  it should "have all interest measures" in {
    val im = task1.fetchInterestMeasures
    im should contain(Lift(1.3))
    im should contain(Confidence(0.1))
    im should contain(Support(0.01))
    im should contain(Limit(100))
    im should contain(RuleLength(3))
    im should contain(CBA)
  }

  "PMML Task 2" should "not have optional interest measures" in {
    task2.fetchInterestMeasures exists {
      case Lift(_) | Limit(_) | RuleLength(_) | CBA  => true
      case _ => false
    } shouldBe false
  }

}
