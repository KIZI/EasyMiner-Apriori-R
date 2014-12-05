import sbt._
import sbt.Keys._
import com.mojolly.scalate.ScalatePlugin._
import ScalateKeys._

object build extends Build {  
  val templateSettings = scalateSettings ++ Seq(
    scalateOverwrite := true,
    scalateTemplateConfig in Compile <<= (baseDirectory) { base =>
      Nil
    }
  )

  lazy val root = Project("root", file(".")).settings(templateSettings:_*)
}