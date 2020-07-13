package com.pinkstack.realestate.clustering3

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, UnreachableMember}
import com.pinkstack.realestate.clustering3.Domain.NodeID

class ClusterListener(nodeID: NodeID, cluster: Cluster) extends Actor with ActorLogging {
  override def preStart(): Unit =
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])

  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = {
    case x: Any =>
      println("~~~>")
      println(x)
      println("~~~>")
  }
}
