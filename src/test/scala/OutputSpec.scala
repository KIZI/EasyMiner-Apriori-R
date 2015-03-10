import cz.vse.easyminer.miner.AND
import cz.vse.easyminer.miner.ARule
import cz.vse.easyminer.miner.BadOutputData
import cz.vse.easyminer.miner.ContingencyTable
import cz.vse.easyminer.miner.FixedValue
import cz.vse.easyminer.miner.Value
import cz.vse.easyminer.miner.impl.ARuleText
import cz.vse.easyminer.miner.impl.BoolExpressionShortText
import cz.vse.easyminer.miner.impl.MySQLDatasetBuilder
import cz.vse.easyminer.miner.impl.PMMLResult
import cz.vse.easyminer.miner.impl.RArule
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest._

@DoNotDiscover
class OutputSpec extends FlatSpec with Matchers with ConfOpt {

  import DBSpec._

  val mdb = new MySQLDatasetBuilder {}

  "RArule" should "should return correct arules from the R string representation" in {
    def newcache = collection.mutable.Map.empty[RArule.State, Boolean]
    mdb.buildAndExecute(dbserver, dbname, dbuser, dbpassword, tableName) { dataset =>
      RArule.unapply("age=51,age=20,age=30=age<20,age>20")(dataset, newcache).toString shouldBe "Some(Some(AND(Value(FixedValue(age=20,age=30,age<20,age>20)),Value(FixedValue(age,51)))))"
      RArule.unapply("cílová proměnná=good=bad=bad,cílová proměnná=good=good,age=20,age=30=age=[20,30],age=20,age=30=age<20,age>20")(dataset, newcache).toString shouldBe "Some(Some(AND(AND(AND(Value(FixedValue(age=20,age=30,age<20,age>20)),Value(FixedValue(age=20,age=30,age=[20,30]))),Value(FixedValue(cílová proměnná=good,good))),Value(FixedValue(cílová proměnná=good,bad=bad)))))"
      val cache = newcache
      RArule.unapply("age=20,age=30,district=Jihlava")(dataset, cache).toString shouldBe "Some(Some(AND(AND(Value(FixedValue(district,Jihlava)),Value(FixedValue(age,30))),Value(FixedValue(age,20)))))"
      val expectedCache: Set[(RArule.State, Boolean)] = Set(RArule.SearchValue("district", "Jihlava") -> true, RArule.SearchValue("age", "30") -> true, RArule.SearchName("age") -> true, RArule.SearchValue("age", "20") -> true, RArule.SearchName("district") -> true)
      cache.forall(expectedCache.apply) shouldBe true
    }
  }

  it should "throw an exception for invalid R arules" in {
    def newcache = collection.mutable.Map.empty[RArule.State, Boolean]
    mdb.buildAndExecute(dbserver, dbname, dbuser, dbpassword, tableName) { dataset =>
      intercept[BadOutputData] {
        RArule.unapply("name1=value1,name2=value2")(dataset, newcache)
      }
      intercept[BadOutputData] {
        RArule.unapply("age=500")(dataset, newcache)
      }
      intercept[BadOutputData] {
        RArule.unapply("undefined=50")(dataset, newcache)
      }
      intercept[BadOutputData] {
        RArule.unapply("age=20,salary")(dataset, newcache)
      }
      intercept[BadOutputData] {
        RArule.unapply("age")(dataset, newcache)
      }
      intercept[BadOutputData] {
        RArule.unapply("age,salary=8363")(dataset, newcache)
      }
    }
  }

  it should "have right visualisation" in {
    val pmml = (new PMMLResult(Seq(ARule(Some(Value(FixedValue("name1", "value1")) AND Value(FixedValue("name2", "value2"))  AND Value(FixedValue("name3", "value3"))), Value(FixedValue("name4", "value4")), Set.empty, ContingencyTable(5, 10, 15, 20)))) with ARuleText with BoolExpressionShortText).toPMML
    pmml should include ("<Text>name1(value1) & name2(value2) & name3(value3) &rarr; name4(value4)</Text>")
  }

}
