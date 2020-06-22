package com.pinkstack.realestate

import akka.http.scaladsl.model.{HttpRequest, Uri}
import io.lemonlabs.uri.Url

object Domain {
  type CategorySlug = String

  class CategoryRequest(val request: HttpRequest, val slug: CategorySlug) {
    override def toString: CategorySlug =
      s"CategoryRequest ${request.uri}"
  }

  class EstateRequest(val request: HttpRequest, val categorySlug: CategorySlug) {
    override def toString: CategorySlug =
      s"EstateRequest ${request.uri}"
  }

  implicit val lemonLabsUrlToAkkaUrl: Url => Uri = url => Uri(url.toString())

  case class Estate(sourceUri: Uri, title: String)

}
