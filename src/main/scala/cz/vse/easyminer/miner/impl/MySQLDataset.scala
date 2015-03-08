package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.Dataset
import cz.vse.easyminer.miner.DatasetBuilder
import java.util.UUID
import scalikejdbc._

object MySQLDatasetBuilder {

  Class.forName("com.mysql.jdbc.Driver")

  GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(
    enabled = true,
    singleLineMode = true,
    printUnprocessedStackTrace = false,
    stackTraceDepth = 15,
    logLevel = 'info,
    warningEnabled = true,
    warningThresholdMillis = 3000L,
    warningLogLevel = 'warn
  )

  def makeCpName = UUID.randomUUID.toString

}

trait MySQLDatasetBuilder extends DatasetBuilder {

  def buildAndExecute[T](dbServer: String, dbName: String, dbUser: String, dbPass: String, dbTableName: String)(dbq: Dataset => T) = {
    val cpName = MySQLDatasetBuilder.makeCpName
    try {
      ConnectionPool.add(cpName, s"jdbc:mysql://$dbServer:3306/$dbName?characterEncoding=utf8", dbUser, dbPass)
      val result = dbq(new MySQLDataset(() => NamedDB(cpName), dbTableName))
      result
    } finally {
      ConnectionPool.close(cpName)
    }
  }

}

class MySQLDataset(db: () => NamedDB, dbTableName: String) extends Dataset {

  def fetchValuesBySelectAndColName(select: String, col: String) = db() readOnly (implicit session =>
    SQL.apply(s"SELECT DISTINCT $select FROM `$dbTableName`").map(_.stringOpt(col)).list.apply)

  def fetchValuesByColName(col: String) = db() readOnly (implicit session =>
    SQL.apply(s"SELECT DISTINCT `$col` FROM `$dbTableName`").map(_.string(col)).list.apply)

  def fetchCount = db() readOnly (implicit session =>
    SQL.apply(s"SELECT COUNT(*) AS count FROM `$dbTableName`").map(_.int("count")).first.apply.getOrElse(0))

  def hasValueByColName(col: String, value: String) = db() readOnly (implicit session =>
    SQL.apply(s"SELECT `$col` FROM `$dbTableName` WHERE `$col` LIKE '$value' LIMIT 1").map(_ => true).first.apply.getOrElse(false))

  def hasColName(col: String) = db() readOnly (implicit session =>
    SQL.apply("SELECT DATABASE() AS dbname").map(_.string("dbname")).first.apply.map(dbName =>
      SQL.apply(s"SELECT `COLUMN_NAME` FROM `INFORMATION_SCHEMA`.`COLUMNS` WHERE `TABLE_SCHEMA`='$dbName' AND `TABLE_NAME`='$dbTableName' AND `COLUMN_NAME`='$col' LIMIT 1").map(_ => true).first.apply.getOrElse(false))
      .getOrElse(false))
}
