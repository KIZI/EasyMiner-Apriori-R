import cz.vse.easyminer.util.Template
import org.scalatest._
import scala.xml._
import cz.vse.easyminer.util.BasicFunction._

class TemplateSpec extends FlatSpec with TemplateOpt with Matchers {

  "Template" should "have defaultBasePath /cz/vse/easyminer/" in {
    Template.defaultBasePath should be("/cz/vse/easyminer/")
  }

  it should "return template xml string by name" in {
    inputpmml should be('defined)
  }

}

trait TemplateOpt extends ConfOpt {
  
  implicit val basePath = "/"
  
  def inputpmml = XML.loadString(
    Template(
      "InputPMML.mustache",
      Map(
        "dbserver" -> dbserver,
        "dbname" -> dbname,
        "dbuser" -> dbuser,
        "dbpassword" -> dbpassword,
        "dbtable" -> dbtable
      )
    )
  ).find(_.label == "PMML")

}
