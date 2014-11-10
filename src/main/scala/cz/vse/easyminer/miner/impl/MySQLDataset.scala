package cz.vse.easyminer.miner.impl

import java.util.UUID
import scalikejdbc._

object MySQLDataset {
  
  Class.forName("com.mysql.jdbc.Driver")
  
  def apply[T](dbServer: String, dbName: String, dbUser: String, dbPass: String, dbTableName: String)(dbq: MySQLDataset => T) = {
    val cpName = UUID.randomUUID.toString
    ConnectionPool.add(cpName, s"jdbc:mysql://$dbServer:3306/$dbName", dbUser, dbPass)
    val result = dbq(new MySQLDataset(() => NamedDB(cpName), dbTableName))
    ConnectionPool.close(cpName)
    result
  }
  
}
  
class MySQLDataset private (db: () => NamedDB, dbTableName: String) { 
  
  def fetchValuesByColName(col: String) = db() readOnly (implicit session =>
    SQL.apply(s"SELECT DISTINCT $col FROM $dbTableName").map(_.string(col)).list.apply
  )
  
}
