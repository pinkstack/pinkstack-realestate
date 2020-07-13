package com.pinkstack.realestate.clustering

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import com.typesafe.scalalogging.LazyLogging

object Domain {

  sealed trait StoreMessage

  case class AddItem(item: String) extends StoreMessage

  case class RemoveItem(item: String) extends StoreMessage

  case object ValidateCart extends StoreMessage

}

object StoreSystemApp extends LazyLogging {

  import Domain._

  def main(args: Array[String]): Unit = {
    logger.info("StoreSystemApp booting,...")

    val storeRootActor = ActorSystem(
      Behaviors.receiveMessage[StoreMessage] { message =>
        message match {
          case AddItem(item) => println(s"Adding item to cart ${item}")
          case RemoveItem(item) => println(s"Removing item ${item}")
          case ValidateCart => println("cart is good")
        }
        Behaviors.same
      }, "storeRootActor"
    )

    storeRootActor ! AddItem("cake")
    storeRootActor ! AddItem("beer")


    val storeRootActorMutable = ActorSystem(
      Behaviors.setup[StoreMessage] { ctx =>
        // Local state
        var items: Set[String] = Set()

        Behaviors.receiveMessage[StoreMessage] { message =>
          message match {
            case AddItem(item) =>
              items += item
              println(s"Item ${item} added.")
            case RemoveItem(item) =>
              items -= item
              println(s"Removed item ${item}")
            case ValidateCart => println("cart is good")
          }
          Behaviors.same
        }
      }
      , "storeRootActorMutalbe"
    )

    def shoppingBehaviour(items: Set[String]): Behavior[StoreMessage] =
      Behaviors.receiveMessage[StoreMessage] {
        case AddItem(item) =>
          println("adding")
          shoppingBehaviour(items + item)
        case RemoveItem(item) =>
          println("removing")
          shoppingBehaviour(items - item)
        case ValidateCart =>
          Behaviors.same
      }

    storeRootActorMutable ! AddItem("coke")
    storeRootActorMutable ! AddItem("coke")
    storeRootActorMutable ! AddItem("beer")

    val rootOnlineActor = ActorSystem(
      Behaviors.setup[StoreMessage] { ctx =>
        ctx.spawn(shoppingBehaviour(Set()), "otoCart")
        Behaviors.empty
      }, "onlineStore"
    )

    rootOnlineActor ! AddItem("xxx")
  }
}
