import cz.vse.easyminer.miner.ARule
import cz.vse.easyminer.miner.AllValues
import cz.vse.easyminer.miner.Confidence
import cz.vse.easyminer.miner.ContingencyTable
import cz.vse.easyminer.miner.FixedValue
import cz.vse.easyminer.miner.Lift
import cz.vse.easyminer.miner.Limit
import cz.vse.easyminer.miner.MinerTask
import cz.vse.easyminer.miner.RScript
import cz.vse.easyminer.miner.Support
import cz.vse.easyminer.miner.Value
import cz.vse.easyminer.miner.impl.ARuleText
import cz.vse.easyminer.miner.impl.AprioriRProcess
import cz.vse.easyminer.miner.impl.BoolExpressionText
import cz.vse.easyminer.miner.impl.DBOptsPMML
import cz.vse.easyminer.miner.impl.MySQLDatasetBuilder
import cz.vse.easyminer.miner.impl.MySQLQueryBuilder
import cz.vse.easyminer.miner.impl.PMMLResult
import org.scalatest._

class MineSpec extends FlatSpec with Matchers with DBSpec {

  lazy val R = new RScript {
    val rServer = rserveAddress
    val rPort = rservePort
  }

  lazy val process = new AprioriRProcess(
    "RAprioriWithMySQL.mustache",
    jdbcdriver,
    rserveAddress,
    rservePort
  ) with MySQLDatasetBuilder with MySQLQueryBuilder with DBOptsPMML {
    val pmml = inputpmml(tableName).get
  }

  "R Script with UTF8+space select query and consequents" should "return one association rule" ignore {
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

  "R Script with small support and confidence" should "return many results" ignore {
    R.eval(
      rscript(
        tableName,
        "*",
        """ "cílová proměnná=splaceno bez problémů","cílová proměnná=dobře splácející","cílová proměnná=špatně splácející","cílová proměnná=definitivně ztraceno" """.trim,
        0.3,
        0.001
      )
    ) should have length 24855
  }

  "AprioriRProcess" should "mine" ignore {
    val x = process.mine(
      MinerTask(Value(AllValues("district")), Set(Support(0.01), Confidence(0.9)), Value(AllValues("cílová proměnná")))
    ) match {
        case Seq(ARule(Some(Value(FixedValue("district", "Liberec"))), Value(FixedValue("cílová proměnná", "špatně splácející")), _, ContingencyTable(63, 0, 3564, 2554))) => true
        case _ => false
      }
    x should be(true)
  }

  it should "mine with lift" ignore {
    val arules = process.mine(
      MinerTask(Value(AllValues("district")), Set(Lift(1.3)), Value(AllValues("cílová proměnná")))
    )
    arules should have length 76
    for (ARule(_, _, im, _) <- arules) {
      im.exists {
        case Lift(v) if v >= 1.3 => true
        case _ => false
      } should be(true)
    }
  }

  it should "mine with limit 100 and return 100 with one empty antecedent" in {
    process.mine(
      MinerTask(Value(AllValues("district")), Set(Support(0.001)), Value(AllValues("cílová proměnná")))
    ) should have length 186
    val limitedResult = process.mine(
      MinerTask(Value(AllValues("district")), Set(Limit(100), Support(0.001)), Value(AllValues("cílová proměnná")))
    )
    limitedResult should have length 100
    val emptyAntecedent = limitedResult.filter(_.antecedent.isEmpty)
    emptyAntecedent should have length 1
    val pmml = (new PMMLResult(emptyAntecedent) with ARuleText with BoolExpressionText).toPMML
    pmml should include("<Text>()</Text>")
    pmml should include("<FieldRef></FieldRef>")
    pmml should include("<CatRef></CatRef>")
    pmml should include(""" <FourFtTable a="3627" b="2554" c="0" d="0" """.trim)
    process.mine(
      MinerTask(Value(AllValues("district")), Set(Limit(100), Support(0.01)), Value(AllValues("cílová proměnná")))
    ) should have length 22
  }

}