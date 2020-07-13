package com.pinkstack.realestate.clustering3

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.{Cluster, Member}
import com.pinkstack.realestate.clustering3.Domain.NodeID

class ClusterManager(nodeID: NodeID) extends Actor with ActorLogging {
  val cluster: Cluster = Cluster(context.system)
  val listener: ActorRef = context.actorOf(Props(classOf[ClusterListener], nodeID, cluster), "clusterListener")

  import ClusterManager._

  override def receive: Receive = {
    case GetMembers =>
      sender ! MembersCollected {
        cluster.state.members.toList.map { member: Member =>
          Map[String, String](
            "address" -> member.address.toString,
            "status" -> member.status.toString
          )
        }
      }
  }
}

object ClusterManager {

  sealed trait ClusterMessage

  sealed trait ClusterEvent

  case object GetMembers extends ClusterMessage

  case class MembersCollected(members: List[Map[String, String]]) extends ClusterEvent

}