package com.pinkstack.realestate

import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

object ScraperApp extends App with AkkaApp with LazyLogging {
  val s = Source(NepClient.seedCategoryRequests)
    .mapAsync(4)(NepClient.fetchCategoryRequests)
    .flatMapConcat(requests => Source(requests))

    .collect {
      case request: EstateRequest =>
        NepClient.fetchEstateListing(request)
      case categoryPageRequest: CategoryPageRequest =>
        Future.successful("ok")
    }
    .mapAsync(5)(identity)
    .filterNot(_ == "ok")
    // .toMat(Sink.foreach(println))(Keep.right)
    .runWith(Sink.foreach(println))
    .onComplete { _ =>
      println("completed.")
      system.terminate()
    }
}
