package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.BadInputData

trait PMMLMySQL {

  val pmml: scala.xml.Elem
  
  lazy val (dbServer, dbName, dbUser, dbPass, dbTableName) = {
    val extensions = (pmml \ "Header" \ "Extension").map(ext =>
      (ext \ "@name").text -> (ext \ "@value").text
    ).toMap
    List("database-server", "database-name", "database-user", "database-password", "dataset", "database-type").map(x => extensions.getOrElse(x, "")).filter(!_.isEmpty) match {
      case a @ List(dbServer, dbName, dbUser, dbPass, dbTableName, "mysql") => (dbServer, dbName, dbUser, dbPass, dbTableName)
      case x => throw new BadInputData(s"Missing data to prepare dataset. Given: $x")
    }
  }
  
}
