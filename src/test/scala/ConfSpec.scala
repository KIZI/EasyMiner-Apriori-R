import com.github.kxbmap.configs._
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import cz.vse.easyminer.util.Conf
import org.scalatest._

class ConfSpec extends FlatSpec with Matchers with ConfOpt {

  "Conf.get" should "return exception with empty string" in {
    intercept[ConfigException.BadPath] {
      Conf().get[String]("")
    }
  }

  it should "return string for rest.address" in {
    Conf().get[String]("rest.address") should not be empty
  }

  it should "return int for rest.port" in {
    val x = Conf().get[Int]("rest.port")
    x should be > 0
  }

  it should "return string for r-miner.rserve-address" in {
    rserveAddress should not be empty
  }

  it should "return string for jdbc-driver-dir-absolute-path" in {
    Conf().get[String]("r-miner.jdbc-driver-dir-absolute-path") should not be empty
  }

  it should "return int or exception Missing for r-miner.rserve-port" in {
    try {
      rservePort should be > 0
    } catch {
      case _: ConfigException.Missing => fail()
    }
  }

  "Conf.opt" should "be None for unexisted attribute" in {
    Conf().opt[String]("unexisted") should be(None)
  }

  it should "be Some(String) for rest.address" in {
    Conf().opt[String]("rest.address") should be('defined)
  }

  "Conf.getOrElse" should "return default string for an unexisted attribute" in {
    Conf().getOrElse[String]("unexisted", "default") should be("default")
  }

  it should "not be default string for rest.address" in {
    Conf().getOrElse[String]("rest.address", "default") should not be ("default")
  }

}

trait ConfOpt {

  val testconfig = new Conf(new EnrichTypesafeConfig(ConfigFactory.load("test")))
  
  def rserveAddress = Conf().get[String]("r-miner.rserve-address")
  def rservePort = Conf().get[Int]("r-miner.rserve-port")
  def dbserver = testconfig.get[String]("db.server")
  def dbuser = testconfig.get[String]("db.user")
  def dbpassword = testconfig.get[String]("db.password")
  def dbname = testconfig.get[String]("db.name")
  def dbtable = testconfig.get[String]("db.table")
  
}