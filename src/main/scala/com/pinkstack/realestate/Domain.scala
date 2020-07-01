package com.pinkstack.realestate

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

  implicit val lemonLabsUrlToAkkaUrl: Url => Uri = url => Uri(url.toString())

  case class Estate(title: String,
                    price: BigDecimal,
                    refNumber: String,
                    sourceUri: Option[String] = None,
                    categories: Map[String, String] = Map.empty[String, String],
                    locationDetails: Map[String, Vector[String]] = Map.empty)

}
