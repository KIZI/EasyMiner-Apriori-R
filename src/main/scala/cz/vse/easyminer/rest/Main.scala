package cz.vse.easyminer.rest

import akka.actor.ActorSystem
import cz.vse.easyminer.miner.MinerTask
import cz.vse.easyminer.miner.impl.ARuleText
import cz.vse.easyminer.miner.impl.AprioriRProcess
import cz.vse.easyminer.miner.impl.BoolExpressionText
import cz.vse.easyminer.miner.impl.MySQLDataset
import cz.vse.easyminer.miner.impl.MySQLDatasetBuilder
import cz.vse.easyminer.miner.impl.MySQLQueryBuilder
import cz.vse.easyminer.miner.impl.PMMLMySQL
import cz.vse.easyminer.miner.impl.PMMLResult
import cz.vse.easyminer.miner.impl.PMMLTask
import spray.routing.Directives
import spray.routing.SimpleRoutingApp

object Main extends App with SimpleRoutingApp with Aaa {
//  implicit val system = ActorSystem("easyminer-rest-system")
  
//  val conn = new RConnection("192.168.137.128", 6311)
//  println(conn.eval("rnorm(10)").asDoubles.toList)
//  conn.close
//
//  println(Template.apply("RAprioriWithMySQL.mustache"))
  val _pmml = xml.XML.loadFile("input.pmml.xml")
  val task = new PMMLTask(_pmml)
  val process = new AprioriRProcess("RAprioriWithMySQL.mustache", "/home/venca/RWorks/mysql-jdbc", "192.168.137.128") with PMMLMySQL with MySQLDatasetBuilder with MySQLQueryBuilder {
    val pmml = _pmml 
  }
  val minertask = MinerTask(task.fetchAntecedent, task.fetchInterestMeasures, task.fetchConsequent)
  println(minertask)
  val result = process.mine(minertask)
  println(result.size)
  val pmmlresult = new PMMLResult with ARuleText with BoolExpressionText
  println(pmmlresult.toPMML(result))
  //println(.prepareDataset(aa => aa.fetchValuesByColName("author")))
  
//  startServer(interface = "89.177.166.161", port = 8080) {
//    pathPrefix("api") {
//      path("v1" ~ Slash.?) {
//        my
//      }
//    }
//  }
  
}

trait Aaa extends Directives {
  val my = get {
    complete {
      <a>aaa</a>
    }
  }
}