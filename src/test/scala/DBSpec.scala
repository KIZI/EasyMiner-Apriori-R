import java.io.StringReader
import org.scalatest._
import scalikejdbc._
import scala.util.Random
import cz.vse.easyminer.util.BasicFunction._
import cz.vse.easyminer.util.ScriptRunner

class DBSpec extends Suites(new DatasetSpec, new MineSpec, new OutputSpec) with BeforeAndAfterAll with ConfOpt with TemplateOpt {

  import DBSpec._
  
  override def beforeAll() {
    DBSpec
    ConnectionPool.singleton(
      "jdbc:mysql://" + dbserver + ":3306/" + dbname + "?characterEncoding=utf8",
      dbuser,
      dbpassword,
      ConnectionPoolSettings(
        initialSize = 10,
        maxSize = 10,
        connectionTimeoutMillis = 1000L,
        validationQuery = "select 1+1"
      )
    )
    val tableExists = DB readOnly { implicit session =>
      sql"SHOW TABLES LIKE $originalTableName".map(_ => true).first().apply
    }
    using(ConnectionPool.borrow()) { conn =>
      if (tableExists.isEmpty) tryClose(new StringReader(datasetSql(originalTableName)))(new ScriptRunner(conn, false, false).runScript)
      conn.createStatement.execute(s"RENAME TABLE `$originalTableName` TO `$tableName`")
    }
  }

  override def afterAll() {
    using(ConnectionPool.borrow()) { conn =>
      conn.createStatement.execute(s"RENAME TABLE `$tableName` TO `$originalTableName`")
    }
    ConnectionPool.close()
  }

}

object DBSpec {
  Class.forName("com.mysql.jdbc.Driver")
  GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(enabled = false)

  val originalTableName = "testdata"

  val tableName = {
    val allowedAsciiChars = List(65 to 90, 97 to 122).flatMap(_.toList).map(_.toChar).toIndexedSeq
    val allowedNonAsciiChars = "ěščřžýáíéůúĚŠČŘŽÝÁÍÉ".toIndexedSeq
    0.until(10).foldLeft("") {
      (r, x) =>
        val sep = if (x == 5) " " else ""
        val charList = if (x % 2 == 0) allowedAsciiChars else allowedNonAsciiChars
        r + sep + charList(Random.nextInt(charList.length))
    }
  }
}