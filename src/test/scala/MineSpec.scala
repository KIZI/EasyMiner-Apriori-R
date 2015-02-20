import cz.vse.easyminer.miner.ARule
import cz.vse.easyminer.miner.AllValues
import cz.vse.easyminer.miner.Confidence
import cz.vse.easyminer.miner.ContingencyTable
import cz.vse.easyminer.miner.FixedValue
import cz.vse.easyminer.miner.MinerTask
import cz.vse.easyminer.miner.RScript
import cz.vse.easyminer.miner.Support
import cz.vse.easyminer.miner.Value
import cz.vse.easyminer.miner.impl.AprioriRProcess
import cz.vse.easyminer.miner.impl.DBOptsPMML
import cz.vse.easyminer.miner.impl.MySQLDatasetBuilder
import cz.vse.easyminer.miner.impl.MySQLQueryBuilder
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
        0.3,
        0.001
      )
    ) should have length 24855
  }

  "AprioriRProcess" should "mine" in {
    process.mine(
      MinerTask(Value(AllValues("district")), Set(Support(0.01), Confidence(0.9)), Value(AllValues("cílová proměnná")))
    ) should matchPattern {
        case Seq(ARule(Value(FixedValue("district", "Liberec")), Value(FixedValue("cílová proměnná", "špatně splácející")), _, ContingencyTable(63, 0, 3564, 2554))) =>
      }
  }

}