package com.pinkstack.realestate.scraper

import akka.http.scaladsl.model.{HttpRequest, Uri}
import io.lemonlabs.uri.Url

object Domain {
  type CategorySlug = String

  case class CategoryRequest(request: HttpRequest, slug: CategorySlug) {
    override def toString: CategorySlug =
      s"CategoryRequest ${request.uri}"
  }

  case class EstateRequest(request: HttpRequest, categorySlug: CategorySlug) {
    override def toString: CategorySlug =
      s"EstateRequest ${request.uri}"
  }

  case class Location(latitude: Double, longitude: Double)

  case class Estate(title: String,
                    refNumber: String,
                    price: Double,
                    visibility: Boolean,
                    scrapedFromUrl: String,
                    locationDetails: Map[String, Vector[String]] = Map.empty,
                    categories: Map[String, String] = Map.empty[String, String],
                    location: Option[Location] = None)
}

object Implicits {
  implicit val lemonLabsUrlToAkkaUrl: Url => Uri = url => Uri(url.toString())
}