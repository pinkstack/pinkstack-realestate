package com.pinkstack.realestate.clustering

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberEvent
import com.typesafe.scalalogging.LazyLogging

class ExampleClusterListener extends Actor with ActorLogging {
  val cluster = Cluster(context.system)

  override def receive: Receive = {
    case event: MemberEvent =>
      log.info(s"Received ${event}")
  }
}

class PingActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case s: String =>
      log.info(s"Received $s in ${self}")
  }
}

object MainApp extends App with LazyLogging {
  implicit val system = ActorSystem("estateApp")
  implicit val context = system.dispatcher

  val pingA = system.actorOf(Props[PingActor])
  pingA ! "Hello Oto"

  println("This is the MainApp")
}
