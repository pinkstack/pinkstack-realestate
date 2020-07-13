/**
 * Author: Oto Brglez - <otobrglez@gmail.com>
 */
package com.pinkstack.realestate.cluster

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.cluster.ClusterEvent.MemberEvent
import akka.cluster.typed._
import com.monovore.decline._
import com.typesafe.config.{Config, ConfigFactory}

object ClusterListener {

  sealed trait Event

  case class ClusterChange(event: MemberEvent) extends Event

  def apply(): Behavior[Event] = Behaviors.setup[Event] { context =>
    val clusterEvents: ActorRef[MemberEvent] = context.messageAdapter(ClusterChange)
    Cluster(context.system).subscriptions ! Subscribe(clusterEvents, classOf[MemberEvent])

    Behaviors.receiveMessage {
      case ClusterChange(event: MemberEvent) =>
        context.log.info("🐝 {}", event)
        Behaviors.same
    }
  }
}

object Node {
  def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    context.spawn(ClusterListener(), "ClusterListener")
    Behaviors.empty
  }
}

object System {
  val configuration: Int => Config = port =>
    ConfigFactory.parseString(
      s"""akka.remote.artery.canonical.port=$port""".stripMargin)
      .withFallback(ConfigFactory.load("clusteringV4.conf"))

  def apply(port: Int): ActorSystem[Nothing] =
    ActorSystem[Nothing](Node(), "AppV4", configuration(port))
}

object NodeApp extends CommandApp(
  name = "node", header = "Boots up a single (seed) cluster node",
  main = for {
    port <- Opts.option[Int]("port", "port number").withDefault(0)
  } yield System(port)
)