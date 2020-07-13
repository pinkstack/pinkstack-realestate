package com.pinkstack.realestate.clustering

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, MemberRemoved, MemberUp, UnreachableMember}
import com.typesafe.scalalogging.LazyLogging
import com.typesafe.config.ConfigFactory

class ClusterListenerActor extends Actor with ActorLogging {
  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self,
      initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember]
    )
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def receive: Receive = {
    case MemberUp(member) =>
      log.info("Member is up {}", member.address)
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable {}", member.address)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member removed {} after {}", member.address, previousStatus)
    case event: MemberEvent =>
      log.info("Got event {}", event)
  }
}

object ClusteringV2 extends LazyLogging {
  def main(args: Array[String]): Unit = {
    logger.info("Clustering demo")

    val clusteringConfig = ConfigFactory.load(
      """
        |akka {
        | actor {
        |   provider = "cluster"
        | }
        | remote.artery {
        |    canonical {
        |      hostname = "0.0.0.0"
        |      port = 2551
        |    }
        | }
        | cluster {
        |    seed-nodes = [
        |      "akka://ClusterSystem@127.0.0.1:2551",
        |      "akka://ClusterSystem@127.0.0.1:2552"]
        |
        |   downing-provider-class = "akka.cluster.sbr.SplitBrainResolverProvider"
        | }
        |}
        |""".stripMargin)

    println(clusteringConfig)
  }
}
