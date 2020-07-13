package com.pinkstack.realestate.clustering3

import akka.actor._
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import akka.http.scaladsl._
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.pinkstack.realestate.clustering3.ClusterManager.{GetMembers, MembersCollected}
import com.typesafe.scalalogging.LazyLogging
import akka.pattern.ask
import akka.pattern.pipe
import akka.pattern.PipeToSupport
import akka.util.Timeout
import com.pinkstack.realestate.Domain.{Estate, EstateRequest}
import com.pinkstack.realestate.{Configuration, NepClient}
import io.lemonlabs.uri.Url

import Domain._


trait ServerRoutes {
  this: LazyLogging =>

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  implicit val system: ActorSystem
  implicit val dispatcher: ExecutionContext

  def stopSystem: Future[String] = for {
    _ <- system.terminate()
    _ = logger.info("System terminated.")
  } yield "Terminated"

  val fetchEstate: (ActorRef, String) => Future[Option[Estate]] = (nodeActor, url) =>
    (nodeActor ? FetchEstate(url)) (3.seconds).mapTo[Option[Estate]]

  val getMembers: ActorRef => Future[MembersCollected] = { nodeActorRef =>
    (nodeActorRef ? GetMembers) (3.seconds).mapTo[MembersCollected]
  }
  val scrapeAll: ActorRef => Future[Map[String, String]] = { nodeActorRef =>
    nodeActorRef ! ScrapeAll
    Future.successful(Map[String, String]("scraping" -> "started,..."))
  }

  def basicRoutes(nodeActorRef: ActorRef): Route = {
    concat(
      pathSingleSlash(get(complete(StatusCodes.OK, "ok"))),
      path("stop")(get(complete(stopSystem))),
      path("fetchEstate") {
        parameters(Symbol("url").as[String]) { (url: String) =>
          onSuccess(fetchEstate(nodeActorRef, url)) {
            case Some(estate) => complete(StatusCodes.OK, estate)
            case None => complete(StatusCodes.BadRequest, Map("error" -> "Sorry, fetching failed."))
          }
        }
      },
      path("getMembers")(get {
        onSuccess(getMembers(nodeActorRef)) { result: MembersCollected =>
          complete(Map("members" -> result.members))
        }
      }),
      path("scrape")(get(complete(scrapeAll(nodeActorRef)))
      )
    )
  }
}

object ServerApp extends App with LazyLogging with ServerRoutes {
  val config: Config = ConfigFactory.load("clusteringv3.conf")
  implicit val system: ActorSystem = ActorSystem("mySystem", config)
  implicit val dispatcher: ExecutionContext = system.dispatcher

  val address: String = config.getString("http.ip")
  val port: Int = config.getInt("http.port")

  val node: ActorRef = system.actorOf(Props(classOf[Node], "x"), "node")

  Http().bindAndHandle(basicRoutes(node), address, port)
  logger.info(s"🚀 Node is listening on http://{}:{}", address, port)
  logger.info(s"🛑 Stop it at http://{}:{}/stop", address, port)

  Await.result(system.whenTerminated, Duration.Inf)
}
