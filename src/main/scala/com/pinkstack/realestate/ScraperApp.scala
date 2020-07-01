package com.pinkstack.realestate

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import com.pinkstack.realestate.Domain._
import com.typesafe.scalalogging.LazyLogging
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.ExecutionContextExecutor

object ScraperApp extends App with LazyLogging {
  logger.info("ScraperApp starting.")

  implicit val system: ActorSystem = ActorSystem("scraperAppTwo")
  implicit val context: ExecutionContextExecutor = system.dispatcher
  implicit val configuration: Configuration = Configuration.loadOrThrow

  val estatePrinter: Option[Estate] => Unit = {
    case Some(estate: Estate) =>
      println(estate.asJson.noSpaces)
    case None =>
  }

  NepClient()
    .pipeline
    .runWith(Sink.foreach(estatePrinter))
    .onComplete(_ => system.terminate())
}
