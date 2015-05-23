package cz.vse.easyminer.miner.impl

import cz.vse.easyminer.miner.{AND, AllValues, Attribute, BoolExpression, DatasetQueryBuilder, FixedValue, NOT, OR, Value}

trait MySQLQueryBuilder extends DatasetQueryBuilder {

  private val ToSQLSelect : PartialFunction[(String, String), String] = {
    case(k, "") => s"`$k`"
    case(k, v) => nameAndValueToIf(k, v)
  }
  
  private val ToSQLSelectMap : PartialFunction[(String, String), (String, String)] = {
    case(k, "") => k -> s"`$k`"
    case(k, v) => k -> nameAndValueToIf(k, v)
  }
  
  private def nameAndValueToIf(k: String, v: String) = s"IF($v, `$k`, NULL) AS `$k`"
  
  private def joinSQLMaps(m1 : Map[String, String], m2 : Map[String, String], f: (String, String) => String) = m1.foldLeft(m2){
    case (r, t @ (k, _)) if !r.contains(k) => r + t
    case (r, (k, v)) =>
      val rv = r(k)
      r + (k -> (if (rv.isEmpty || v.isEmpty) "" else f(rv, v)))
  }
  
  private def toSQLMap(exp: BoolExpression[Attribute]) : Map[String, String] = exp match {
    case AND(a, b) => joinSQLMaps(toSQLMap(a), toSQLMap(b), (a, b) => s"($a AND $b)")
    case OR(a, b) => joinSQLMaps(toSQLMap(a), toSQLMap(b), (a, b) => s"($a OR $b)")
    case Value(AllValues(a)) => Map(a -> "")
    case Value(FixedValue(a, v)) => Map(a -> s"`$a` = '$v'")
    case NOT(a) => toSQLMap(a) map {case(k, v) => k -> (if (v.isEmpty) v else s"NOT($v)")}
    case _ => Map.empty
  }
  
  def toSQLSelect(exp: BoolExpression[Attribute]) = toSQLMap(exp).collect(ToSQLSelect)
  
  def toSQLSelectMap(exp: BoolExpression[Attribute]) = toSQLMap(exp).collect(ToSQLSelectMap)
  
}
