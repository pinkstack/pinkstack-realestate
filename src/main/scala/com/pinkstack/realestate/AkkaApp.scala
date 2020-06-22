package com.pinkstack.realestate

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContextExecutor

trait AkkaApp {
  implicit val system: ActorSystem = ActorSystem("scraperApp")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
}
