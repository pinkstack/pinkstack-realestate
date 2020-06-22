package com.pinkstack.realestate

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import com.pinkstack.realestate.Domain._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContextExecutor

object ScraperApp extends App with LazyLogging {
  logger.info("ScraperApp starting.")

  implicit val system: ActorSystem = ActorSystem("scraperAppTwo")
  implicit val context: ExecutionContextExecutor = system.dispatcher

  val estatePrinter: Option[Estate] => Unit = {
    case Some(Estate(uri, title: String)) =>
      println(s"$title at ${uri.toString}")
    case None =>
      println("Fail.")
  }

  NepClient()
    .pipeline
    .runWith(Sink.foreach(estatePrinter))
    .onComplete(_ => system.terminate())
}
