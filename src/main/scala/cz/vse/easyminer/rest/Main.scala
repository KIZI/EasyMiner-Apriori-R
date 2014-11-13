package cz.vse.easyminer.rest

import akka.actor.ActorSystem
import com.typesafe.scalalogging.slf4j.Logger
import cz.vse.easyminer.miner.impl.PMMLMySQLTask
import cz.vse.easyminer.util.Template
import org.fusesource.scalate.TemplateEngine
import org.rosuda.REngine.Rserve.RConnection
import org.slf4j.LoggerFactory
import spray.routing.Directives
import spray.routing.SimpleRoutingApp

object Main extends App with SimpleRoutingApp with Aaa {
  //implicit val system = ActorSystem("easyminer-rest-system")
  
//  val conn = new RConnection("192.168.137.128", 6311)
//  println(conn.eval("rnorm(10)").asDoubles.toList)
//  conn.close
//
//  println(Template.apply("RAprioriWithMySQL.mustache"))
  
  val task = new PMMLMySQLTask(xml.XML.loadFile("input.pmml.xml"))
  println(task.fetchAntecedent)
  println(task.fetchConsequent)
  println(task.fetchInterestMeasures)
  //println(.prepareDataset(aa => aa.fetchValuesByColName("author")))
  
//  startServer(interface = "localhost", port = 8080) {
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