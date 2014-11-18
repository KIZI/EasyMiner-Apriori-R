package cz.vse.easyminer.miner.impl

import java.util.UUID
import scalikejdbc._

object MySQLDataset {
  
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
  
  def apply[T](dbServer: String, dbName: String, dbUser: String, dbPass: String, dbTableName: String)(dbq: MySQLDataset => T) = {
    val cpName = UUID.randomUUID.toString
    try {
      ConnectionPool.add(cpName, s"jdbc:mysql://$dbServer:3306/$dbName", dbUser, dbPass)
      val result = dbq(new MySQLDataset(() => NamedDB(cpName), dbTableName))
      result
    } finally {
      ConnectionPool.close(cpName)
    }
  }
  
}
  
class MySQLDataset private (db: () => NamedDB, dbTableName: String) { 
  
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
