package com.pinkstack.realestate.clustering

import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.discovery.{Discovery, Lookup}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.duration._

object MainApp3 extends LazyLogging {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.parseString(
      s"""
         |akka.discovery.method = aggregate
         |akka.discovery.aggregate {
         |  discovery-methods = ["akka-dns", "config"]
         |}
         |akka.discovery.config.services = {
         |  seeder = {
         |    endpoints = [
         |      {
         |        host = 0.0.0.0
         |        port = 6000
         |      }
         |   ]
         |  },
         |  fetchers = {
         |    endpoints = [
         |      {
         |        host = 0.0.0.0
         |        port = 6001
         |      },
         |      {
         |        host = 0.0.0.0
         |        port = 6002
         |      }
         |    ]
         |  }
         |}
         |akka.cluster.shutdown-after-unsuccessful-join-seed-nodes = 10s
         |akka.coordinated-shutdown.exit-jvm = on
         |""".stripMargin).withFallback(ConfigFactory.load)

    implicit val system = ActorSystem("main3", config)
    implicit val executionContext = system.dispatcher

    val discovery = Discovery(system).discovery

    discovery.lookup(Lookup("fetchers"), 3.seconds).map { a =>
      println(a.getAddresses)
    }

    CoordinatedShutdown(system).addJvmShutdownHook {
      logger.info("Terminating...")
      // clean shutdown of the flow
      logger.info("Terminated... Bye")
    }
  }
}
