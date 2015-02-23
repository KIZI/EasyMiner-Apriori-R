import cz.vse.easyminer.miner.AllValues
import cz.vse.easyminer.miner.Confidence
import cz.vse.easyminer.miner.FixedValue
import cz.vse.easyminer.miner.Lift
import cz.vse.easyminer.miner.Support
import cz.vse.easyminer.miner.Value
import cz.vse.easyminer.miner.impl.AttributeValueNormalizerImpl
import cz.vse.easyminer.miner.impl.PMMLTask
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class InputSpec extends FlatSpec with Matchers with TemplateOpt {

  lazy val task = new PMMLTask(inputpmml2.get) with AttributeValueNormalizerImpl

  "PMML Task" should "filter all quotes" in {
    task.fetchAntecedent should be(Value(AllValues("District")))
    task.fetchConsequent should be(Value(FixedValue("status_s_diakritikou", "Cý")))
  }

  it should "have all interest measures" in {
    val im = task.fetchInterestMeasures
    im should contain(Lift(1.3))
    im should contain(Confidence(0.1))
    im should contain(Support(0.01))
  }

}
