package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.AND
import cz.vse.easyminer.miner.AllValues
import cz.vse.easyminer.miner.Attribute
import cz.vse.easyminer.miner.BadInputData
import cz.vse.easyminer.miner.BoolExpression
import cz.vse.easyminer.miner.FixedValue
import cz.vse.easyminer.miner.MinerProcess
import cz.vse.easyminer.miner.MinerResult
import cz.vse.easyminer.miner.MinerTask
import cz.vse.easyminer.miner.NOT
import cz.vse.easyminer.miner.OR
import cz.vse.easyminer.miner.Value
import org.rosuda.REngine.Rserve.RConnection

class MySQLAprioriRProcess(pmml: scala.xml.Elem, rServer: String, rPort: Int = 6311) extends MinerProcess {
  
  private val (dbServer, dbName, dbUser, dbPass, dbTableName) = {
    val extensions = (pmml \ "Header" \ "Extension").map(ext =>
      (ext \ "@name").text -> (ext \ "@value").text
    ).toMap
    List("database-server", "database-name", "database-user", "database-password", "dataset", "database-type").map(x => extensions.getOrElse(x, "")).filter(!_.isEmpty) match {
      case a @ List(dbServer, dbName, dbUser, dbPass, dbTableName, "mysql") => (dbServer, dbName, dbUser, dbPass, dbTableName)
      case x => throw new BadInputData(s"Missing data to prepare dataset. Given: $x")
    }
  }
  
  //private val conn = new RConnection(rServer, rPort)
  
  def prepareDataset[T]: (MySQLDataset => T) => T = {
    MySQLDataset.apply(dbServer, dbName, dbUser, dbPass, dbTableName) _
  }
  
  object ANDOR {
    def unapply(exp: BoolExpression[Attribute]) = exp match {
      case AND(a, b) => Some(a, b)
      case OR(a, b) => Some(a, b)
      case _ => None
    }
  }
  
  /**
   * udelat antecedent OR consequent - tim zamixujeme vsechny itemsety
   * polozit dotaz na mysql z aktualnim selectem. Udelat select distinct jen pro atributy consequentu. Prepsat do Rka
   */
  def toSQLSelect(exp: BoolExpression[Attribute]) : String = {
    def joinMaps(m1 : Map[String, String], m2 : Map[String, String], f: (String, String) => String) = m1.foldLeft(m2){
      case (r, t @ (k, _)) if !r.contains(k) => r + t
      case (r, (k, v)) => {
          val rv = r(k)
          r + (k -> (if (rv.isEmpty || v.isEmpty) "" else f(rv, v))) 
        }
    }
    def toSQLMap(exp: BoolExpression[Attribute]) : Map[String, String] = exp match {
      case AND(a, b) => joinMaps(toSQLMap(a), toSQLMap(b), (a, b) => s"($a AND $b)")
      case OR(a, b) => joinMaps(toSQLMap(a), toSQLMap(b), (a, b) => s"($a OR $b)")
      case Value(AllValues(a)) => Map(a -> "")
      case Value(FixedValue(a, v)) => Map(a -> s"`$a` = '$v'")
      case NOT(a) => toSQLMap(a) map {case(k, v) => k -> (if (v.isEmpty) v else s"NOT($v)")}
      case _ => Map.empty
    }
    toSQLMap(exp)
    .map{
      case(k, "") => k
      case(k, v) => s"IF($v, `$k`, NULL) AS `$k`"
    }
    .mkString(", ")
  }
  
  def mine(mt: MinerTask) : MinerResult = {
    println(mt.antecedent)
    println(toSQLSelect(mt.antecedent))
    null
  }
  
}
