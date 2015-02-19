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
    warningLogLevel = 'warn)
  
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
    SQL.apply(s"SELECT DISTINCT $select FROM `$dbTableName`").map(_.stringOpt(col)).list.apply
  )
  
  def fetchValuesByColName(col: String) = db() readOnly (implicit session =>
    SQL.apply(s"SELECT DISTINCT `$col` FROM `$dbTableName`").map(_.string(col)).list.apply
  )
  
  def fetchCount = db() readOnly (implicit session =>
    SQL.apply(s"SELECT COUNT(*) AS count FROM `$dbTableName`").map(_.int("count")).first.apply.getOrElse(0)
  )
  
}
