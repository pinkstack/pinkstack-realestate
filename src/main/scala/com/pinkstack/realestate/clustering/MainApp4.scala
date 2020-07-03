package com.pinkstack.realestate.clustering

import akka.actor.typed._
import akka.actor.typed.scaladsl._

object Greeter {

  final case class Greet(whom: String)

  final case class Greeted(whom: String, from: ActorRef[Greet])

  def apply(): Behavior[Greet] = Behaviors.receive { (context, message) =>
    context.log.info(s"Hello ${message.whom}")

    if (message.whom.contains("xxx")) {
      Behaviors.stopped
    } else Behaviors.same
  }
}

object MainApp4 {

  final case class Hello(name: String)

  def apply(): Behavior[Hello] = Behaviors.setup { context =>
    context.log.info("Setup")

    val greeter = context.spawn(Greeter(), "greeter")

    Behaviors.receiveMessage { message =>
      context.log.info(s"Got $message")

      greeter ! Greeter.Greet(s"hello ${message.name}")
      Behaviors.same
    }
  }

  def main(args: Array[String]): Unit = {
    val system: ActorSystem[MainApp4.Hello] = ActorSystem(MainApp4(), "mainSystem")

    system ! Hello("Oto")
    system ! Hello("Martina")
    system ! Hello("xxx")
  }
}
