import cz.vse.easyminer.miner.impl.AttributeValueNormalizerImpl
import cz.vse.easyminer.miner.impl.PMMLTask
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class InputSpec extends FlatSpec with Matchers with TemplateOpt {

  "PMML Task" should "filter all quotes" in {
    val task = new PMMLTask(inputpmml2.get) with AttributeValueNormalizerImpl
    println(task.fetchAntecedent)
    println(task.fetchConsequent)
  }
  
}
