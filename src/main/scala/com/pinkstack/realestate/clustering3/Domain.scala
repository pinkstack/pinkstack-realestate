package com.pinkstack.realestate.clustering3

object Domain {
  type NodeID = String

  sealed trait Command

  case class FetchEstate(url: String) extends Command

  case object ScrapeAll extends Command

}
