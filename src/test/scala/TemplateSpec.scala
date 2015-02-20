import cz.vse.easyminer.util.Template
import org.scalatest._
import scala.xml._
import cz.vse.easyminer.util.BasicFunction._

class TemplateSpec extends FlatSpec with TemplateOpt with Matchers {

  "Template" should "have defaultBasePath /cz/vse/easyminer/" in {
    Template.defaultBasePath should be("/cz/vse/easyminer/")
  }

  it should "have XML Input string by name" in {
    inputpmml("") should be('defined)
  }

  it should "have R script string by name" in {
    rscript("", "", "", 0, 0) should not be empty
  }

}

trait TemplateOpt extends ConfOpt {

  implicit val basePath = "/"

  def inputpmml(tableName: String) = XML.loadString(
    Template(
      "InputPMML.mustache",
      Map(
        "dbserver" -> dbserver,
        "dbname" -> dbname,
        "dbuser" -> dbuser,
        "dbpassword" -> dbpassword,
        "dbtable" -> tableName
      )
    )
  ).find(_.label == "PMML")

  def datasetSql(tableName: String) = Template("dataset.sql.mustache", Map("tableName" -> tableName))

  def rscript(tableName: String, selectQuery: String, consequent: String, confidence: Double, support: Double) = Template(
    "RAprioriWithMySQL.mustache",
    Map(
      "jdbcDriverAbsolutePath" -> jdbcdriver,
      "dbServer" -> dbserver,
      "dbName" -> dbname,
      "dbUser" -> dbuser,
      "dbPassword" -> dbpassword,
      "dbTableName" -> tableName,
      "selectQuery" -> selectQuery,
      "consequent" -> consequent,
      "confidence" -> confidence,
      "support" -> support
    )
  )(Template.defaultBasePath)

}
