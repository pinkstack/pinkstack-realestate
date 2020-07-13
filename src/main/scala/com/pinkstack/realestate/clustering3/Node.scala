package com.pinkstack.realestate.clustering3

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.pinkstack.realestate.clustering3.ClusterManager.GetMembers
import com.pinkstack.realestate.clustering3.Domain.{FetchEstate, NodeID, ScrapeAll}
import com.pinkstack.realestate.{Configuration, NepClient}
import io.lemonlabs.uri.Url

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import akka.pattern.{ask, pipe}

class Node(nodeID: NodeID) extends Actor with ActorLogging {
  val configuration: Configuration = Configuration.loadOrThrow
  val nepClient: NepClient = NepClient(4.seconds)(this.context.system, this.context.system.dispatcher, configuration)
  implicit val executionContext: ExecutionContext = this.context.dispatcher

  val clusterManager: ActorRef = context.actorOf(Props(classOf[ClusterManager], nodeID), "clusterManager")

  override def preStart(): Unit = log.info("preStart of {} in {}", nodeID, self)

  override def receive: Receive = {
    case FetchEstate(url) =>
      log.info("Fetching {}", url)
      nepClient.fetchEstatePageViaUrl(Url.parse(url)) pipeTo sender
    case command@GetMembers =>
      ((clusterManager ? command) (3.seconds)) pipeTo sender
    case ScrapeAll =>

    case x: Any =>
      log.info("Got {} at {}", x, self)
  }
}
