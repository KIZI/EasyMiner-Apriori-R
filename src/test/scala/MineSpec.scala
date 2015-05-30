import cz.vse.easyminer.miner._
import cz.vse.easyminer.miner.impl.ARuleText
import cz.vse.easyminer.miner.impl.AprioriRProcess
import cz.vse.easyminer.miner.impl.BoolExpressionShortText
import cz.vse.easyminer.miner.impl.DBOptsPMML
import cz.vse.easyminer.miner.impl.MinerTaskValidatorImpl
import cz.vse.easyminer.miner.impl.MySQLDatasetBuilder
import cz.vse.easyminer.miner.impl.MySQLQueryBuilder
import cz.vse.easyminer.miner.impl.PMMLResult
import cz.vse.easyminer.miner.impl.RConnectionPoolImpl
import org.scalatest._

@DoNotDiscover
class MineSpec extends FlatSpec with Matchers with ConfOpt with TemplateOpt {

  import DBSpec._

  lazy val R = new RScript {
    val rcp = RConnectionPoolImpl.default
  }

  lazy val process = new AprioriRProcess(
    "RAprioriWithMySQL.mustache",
    jdbcdriver,
    RConnectionPoolImpl.default
  ) with MinerTaskValidatorImpl with MySQLDatasetBuilder with MySQLQueryBuilder with DBOptsPMML {
    val pmml = inputpmml(tableName).get
  }

  "R connection pooling" should "have minIdle = 2 and maxIdle = 10" in {
    val conn = new RConnectionPoolImpl(rserveAddress, rservePort, false)
    def makeBorrowedConnections(num: Int) = (0 until num).map(_ => conn.borrow).toList
    var borrowedConnections = List.empty[BorrowedConnection]
    conn.refresh
    conn.numIdle shouldBe 2
    borrowedConnections = makeBorrowedConnections(1)
    conn.refresh
    conn.numIdle shouldBe 2
    borrowedConnections = borrowedConnections ::: makeBorrowedConnections(3)
    conn.refresh
    conn.numIdle shouldBe 2
    conn.numActive shouldBe 4
    borrowedConnections foreach conn.release
    Thread sleep 2000
    conn.refresh
    conn.numIdle shouldBe 6
    conn.numActive shouldBe 0
    borrowedConnections = makeBorrowedConnections(3)
    conn.numIdle shouldBe 3
    conn.numActive shouldBe 3
    borrowedConnections = borrowedConnections ::: makeBorrowedConnections(9)
    conn.refresh
    conn.numIdle shouldBe 2
    conn.numActive shouldBe 12
    borrowedConnections foreach conn.release
    Thread sleep 2000
    conn.refresh
    conn.numIdle shouldBe 10
    conn.numActive shouldBe 0
    conn.close
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
    x shouldBe true
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
      } shouldBe true
    }
  }

  it should "mine with rule length" in {
    val lengthsAndResults = Seq(
      1 -> 2,
      2 -> 479,
      3 -> 1903
    )
    for ((maxlen, expectedResult) <- lengthsAndResults) {
      val arules = process.mine(
        MinerTask(Value(AllValues("district")) AND Value(AllValues("age")) AND Value(AllValues("salary")), Set(RuleLength(maxlen), Confidence(0.1), Support(0.001)), Value(AllValues("cílová proměnná")))
      )
      arules.length shouldBe expectedResult
    }
  }

  it should "mine with CBA" in {
    val antecedent = Value(AllValues("district")) AND Value(AllValues("age")) AND Value(AllValues("salary"))
    val consequent = Value(AllValues("cílová proměnná"))
    val withoutCba = process.mine(
      MinerTask(antecedent, Set(Confidence(0.01), Support(0.001)), consequent)
    )
    val withCba = process.mine(
      MinerTask(antecedent, Set(CBA, Confidence(0.01), Support(0.001)), consequent)
    )
    val withCbaLimit = process.mine(
      MinerTask(antecedent, Set(CBA, Confidence(0.01), Support(0.001), Limit(100)), consequent)
    )
    val withCbaZero = process.mine(
      MinerTask(antecedent, Set(CBA, Confidence(0.9), Support(0.9)), consequent)
    )
    withoutCba.length shouldBe 2601
    withCba.length shouldBe 554
    withCbaLimit.length shouldBe 100
    withCbaZero.length shouldBe 0
  }

  it should "mine with limit 100 and return 100 with one empty antecedent" in {
    process.mine(
      MinerTask(Value(AllValues("district")), Set(Support(0.001), Confidence(0.1)), Value(AllValues("cílová proměnná")))
    ).length shouldBe 186
    process.mine(
      MinerTask(Value(AllValues("district")), Set(Support(0.9), Confidence(0.9)), Value(AllValues("cílová proměnná")))
    ).length shouldBe 0
    val limitedResult = process.mine(
      MinerTask(Value(AllValues("district")), Set(Limit(100), Support(0.001), Confidence(0.1)), Value(AllValues("cílová proměnná")))
    )
    limitedResult.length shouldBe 100
    val emptyAntecedent = limitedResult.filter(_.antecedent.isEmpty)
    emptyAntecedent.length shouldBe 1
    val pmml = (new PMMLResult(emptyAntecedent) with ARuleText with BoolExpressionShortText).toPMML
    pmml should not include "<Text>()</Text>"
    pmml should not include "<FieldRef></FieldRef>"
    pmml should not include "antecedent="
    pmml should include( """ <FourFtTable a="3627" b="2554" c="0" d="0" """.trim)
    process.mine(
      MinerTask(Value(AllValues("district")), Set(Limit(100), Support(0.01), Confidence(0.1)), Value(AllValues("cílová proměnná")))
    ).length shouldBe 22
  }

  it should "throw an exception due to bad interest measure values" in {
    val badInterestMeasures: Seq[Set[InterestMeasure]] = Seq(
      Set(),
      Set(Support(0.5)),
      Set(Confidence(0.5)),
      Set(Support(1.1), Confidence(0.5)),
      Set(Confidence(0.5), Support(1.1)),
      Set(Support(0.0009), Confidence(0.5)),
      Set(Support(0.5), Confidence(0.0009)),
      Set(Support(0.5), Confidence(0.5), Limit(0)),
      Set(Support(0.5), Confidence(0.5), RuleLength(0))
    )
    for (im <- badInterestMeasures) intercept[BadInputData] {
      process.mine(
        MinerTask(Value(AllValues("district")), im, Value(AllValues("cílová proměnná")))
      )
    }
  }

  it should "throw an exception due to bad attributes for CBA" in {
    val badAttributes: Seq[BoolExpression[Attribute]] = Seq(
      Value(AllValues("cílová proměnná")) AND Value(AllValues("age")),
      Value(AllValues("cílová proměnná")) AND Value(FixedValue("age", "51")),
      Value(*)
    )
    for (attr <- badAttributes) intercept[BadInputData] {
      process.mine(
        MinerTask(Value(AllValues("district")), Set(Support(0.5), Confidence(0.5), CBA), attr)
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