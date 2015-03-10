import cz.vse.easyminer.miner.AND
import cz.vse.easyminer.miner.ARule
import cz.vse.easyminer.miner.AllValues
import cz.vse.easyminer.miner.Attribute
import cz.vse.easyminer.miner.BadInputData
import cz.vse.easyminer.miner.BoolExpression
import cz.vse.easyminer.miner.Confidence
import cz.vse.easyminer.miner.ContingencyTable
import cz.vse.easyminer.miner.FixedValue
import cz.vse.easyminer.miner.InterestMeasure
import cz.vse.easyminer.miner.Lift
import cz.vse.easyminer.miner.Limit
import cz.vse.easyminer.miner.MinerTask
import cz.vse.easyminer.miner.NOT
import cz.vse.easyminer.miner.RScript
import cz.vse.easyminer.miner.Support
import cz.vse.easyminer.miner.Value
import cz.vse.easyminer.miner.impl.ARuleText
import cz.vse.easyminer.miner.impl.AprioriRProcess
import cz.vse.easyminer.miner.impl.BoolExpressionShortText
import cz.vse.easyminer.miner.impl.DBOptsPMML
import cz.vse.easyminer.miner.impl.MinerTaskValidatorImpl
import cz.vse.easyminer.miner.impl.MySQLDatasetBuilder
import cz.vse.easyminer.miner.impl.MySQLQueryBuilder
import cz.vse.easyminer.miner.impl.PMMLResult
import org.scalatest._

@DoNotDiscover
@Ignore
class MineSpec extends FlatSpec with Matchers with ConfOpt with TemplateOpt {

  import DBSpec._
  
  lazy val R = new RScript {
    val rServer = rserveAddress
    val rPort = rservePort
  }

  lazy val process = new AprioriRProcess(
    "RAprioriWithMySQL.mustache",
    jdbcdriver,
    rserveAddress,
    rservePort
  ) with MinerTaskValidatorImpl with MySQLDatasetBuilder with MySQLQueryBuilder with DBOptsPMML {
    val pmml = inputpmml(tableName).get
  }

  "R Script with UTF8+space select query and consequents" should "return one association rule" in {
    R.eval(
      rscript(
        tableName,
        "IF(`district`='Jindřichův Hradec', `district`, NULL) AS `district`, IF((`cílová proměnná`='splaceno bez problémů' OR `cílová proměnná`='dobře splácející'), `cílová proměnná`, NULL) AS `cílová proměnná`",
        """ "cílová proměnná=splaceno bez problémů","cílová proměnná=dobře splácející" """.trim,
        0.3,
        0.001
      )
    ) should have length 2
  }

  "R Script with small support and confidence" should "return many results" in {
    R.eval(
      rscript(
        tableName,
        "*",
        """ "cílová proměnná=splaceno bez problémů","cílová proměnná=dobře splácející","cílová proměnná=špatně splácející","cílová proměnná=definitivně ztraceno" """.trim,
        0.5,
        0.002
      )
    ).length shouldBe 20339
  }

  "AprioriRProcess" should "mine" in {
    val x = process.mine(
      MinerTask(Value(AllValues("district")), Set(Support(0.01), Confidence(0.9)), Value(AllValues("cílová proměnná")))
    ) match {
        case Seq(ARule(Some(Value(FixedValue("district", "Liberec"))), Value(FixedValue("cílová proměnná", "špatně splácející")), _, ContingencyTable(63, 0, 3564, 2554))) => true
        case _ => false
      }
    x should be(true)
  }

  it should "mine with lift" in {
    val arules = process.mine(
      MinerTask(Value(AllValues("district")), Set(Lift(1.3), Confidence(0.1), Support(0.001)), Value(AllValues("cílová proměnná")))
    )
    arules.length shouldBe 76
    for (ARule(_, _, im, _) <- arules) {
      im.exists {
        case Lift(v) if v >= 1.3 => true
        case _ => false
      } should be(true)
    }
  }

  it should "mine with limit 100 and return 100 with one empty antecedent" in {
    process.mine(
      MinerTask(Value(AllValues("district")), Set(Support(0.001), Confidence(0.1)), Value(AllValues("cílová proměnná")))
    ).length shouldBe 186
    val limitedResult = process.mine(
      MinerTask(Value(AllValues("district")), Set(Limit(100), Support(0.001), Confidence(0.1)), Value(AllValues("cílová proměnná")))
    )
    limitedResult.length shouldBe 100
    val emptyAntecedent = limitedResult.filter(_.antecedent.isEmpty)
    emptyAntecedent.length shouldBe 1
    val pmml = (new PMMLResult(emptyAntecedent) with ARuleText with BoolExpressionShortText).toPMML
    pmml should not include ("<Text>()</Text>")
    pmml should not include ("<FieldRef></FieldRef>")
    pmml should not include ("antecedent=")
    pmml should include(""" <FourFtTable a="3627" b="2554" c="0" d="0" """.trim)
    process.mine(
      MinerTask(Value(AllValues("district")), Set(Limit(100), Support(0.01), Confidence(0.1)), Value(AllValues("cílová proměnná")))
    ).length shouldBe 22
  }

  it should "throw an exception due to bad interest measure values" in {
    val badInterestMeasures: Seq[Set[InterestMeasure]] = Seq(
      Set(),
      Set(Support(0.5)),
      Set(Confidence(0.5)),
      Set(Support(1.1), Support(0.5)),
      Set(Support(0.5), Support(1.1)),
      Set(Support(0.0009), Support(0.5)),
      Set(Support(0.5), Support(0.0009)),
      Set(Support(0.5), Support(0.5), Limit(0))
    )
    for (im <- badInterestMeasures) intercept[BadInputData] {
      process.mine(
        MinerTask(Value(AllValues("district")), im, Value(AllValues("cílová proměnná")))
      )
    }
  }

  it should "throw an exception due to quotation marks within values" in {
    val badAntCon: Seq[(BoolExpression[Attribute], BoolExpression[Attribute])] = Seq(
      AND(Value(FixedValue("xy\"", "xy\"")), Value(FixedValue("yx\"", "yx\""))) -> Value(AllValues("cílová proměnná")),
      Value(AllValues("cílová proměnná")) -> AND(Value(FixedValue("xy\"", "xy\"")), Value(FixedValue("yx\"", "yx\""))),
      Value(AllValues("xy\"")) -> Value(AllValues("cílová proměnná")),
      Value(AllValues("xy'")) -> Value(AllValues("cílová proměnná")),
      NOT(Value(AllValues("xy'"))) -> Value(AllValues("cílová proměnná"))
    )
    for ((a, b) <- badAntCon) intercept[BadInputData] {
      process.mine(
        MinerTask(a, Set(Support(0.5), Support(0.5)), b)
      )
    }
  }

}