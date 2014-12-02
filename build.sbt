name := "easyminer-apriori-r"

version := "1.0"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

//fork in run := true

//javaOptions in run ++= Seq("-Duser.country=US", "-Duser.language=en")

//outputStrategy := Some(StdoutOutput)

libraryDependencies ++= {
  val akkaV = "2.3.6"
  val sprayV = "1.3.2"
  Seq(
    "io.spray"            %%  "spray-can"     % sprayV,
    "io.spray"            %%  "spray-routing" % sprayV,
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-slf4j"    % akkaV,
    "org.nuiton.thirdparty" % "REngine" % "1.7-3",
    "org.nuiton.thirdparty" % "Rserve" % "1.7-3",
    "org.scalatra.scalate" %% "scalate-core" % "1.7.0",
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
    "org.slf4j" % "slf4j-simple" % "1.7.7",
    "mysql" % "mysql-connector-java" % "5.1.33",
    "org.scalikejdbc" %% "scalikejdbc"       % "2.2.0"
  )
}
