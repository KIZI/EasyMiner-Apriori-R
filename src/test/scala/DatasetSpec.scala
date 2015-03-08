import cz.vse.easyminer.miner.impl.MySQLDatasetBuilder
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest._

@DoNotDiscover
class DatasetSpec extends FlatSpec with Matchers with ConfOpt {

  import DBSpec._
  
  val mdb = new MySQLDatasetBuilder {}

  "MySQLDatasetBuilder" should "connect and create some connection pool" in {
    mdb.buildAndExecute(dbserver, dbname, dbuser, dbpassword, tableName)(_ => Unit)
  }

  "MySQLDataset" should "execute all its queries" in {
    mdb.buildAndExecute(dbserver, dbname, dbuser, dbpassword, tableName) { dataset =>
      val tv = List("splaceno bez problémů", "definitivně ztraceno", "špatně splácející", "dobře splácející")
      dataset.fetchCount shouldBe 6181
      dataset.fetchValuesBySelectAndColName("`cílová proměnná`", "cílová proměnná").forall(tv.map(Some.apply).contains) shouldBe true
      dataset.fetchValuesByColName("cílová proměnná").forall(tv.contains) shouldBe true
      dataset.hasValueByColName("cílová proměnná", "definitivně ztraceno") shouldBe true
      dataset.hasValueByColName("cílová proměnná", "something else") shouldBe false
      dataset.hasColName("cílová proměnná") shouldBe true
      dataset.hasColName("something else") shouldBe false
    }
  }

}
