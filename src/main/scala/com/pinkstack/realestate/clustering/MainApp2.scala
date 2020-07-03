package com.pinkstack.realestate.clustering

import akka.actor.typed.scaladsl._
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.cluster.typed.Cluster
import akka.cluster.typed.Subscribe
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

object ClusterListener {

  sealed trait Event

  def apply(): Behavior[Event] = Behaviors.setup { ctx =>
    Behaviors.receiveMessage {
      case _ =>
        println("ok")
        Behaviors.same
    }
  }
}

object MainApp2 extends LazyLogging {

  object RootBehavior {
    def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
      context.spawn(ClusterListener(), "ClusterListener")
      Behaviors.empty
    }
  }

  def main(args: Array[String]): Unit = {
    val ports: Seq[Int] = Seq(33200, 33201, 0)
    ports.foreach(boot)
  }

  def boot(port: Int): Unit = {
    val config = ConfigFactory.parseString(
      s"""
         |akka.remote.artery.canonical.host=0.0.0.0
         |akka.remote.artery.canonical.port=$port""".stripMargin).withFallback(ConfigFactory.load)
    ActorSystem[Nothing](RootBehavior(), s"ClusterSystem-$port", config)
  }
}

