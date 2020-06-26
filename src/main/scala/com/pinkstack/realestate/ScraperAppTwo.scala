package com.pinkstack.realestate

import pureconfig._
import pureconfig.generic.auto._

object ScraperAppTwo extends App {
  val config = Configuration.loadOrThrow
  println(config.toString)
}
