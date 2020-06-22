package com.pinkstack.realestate

import akka.http.scaladsl.model.HttpRequest

sealed trait NepRequest {
  def request: HttpRequest
}
sealed trait CategoryRequestDetail


case class EstateRequest(request: HttpRequest) extends NepRequest with CategoryRequestDetail


case class CategoryPageRequest(request: HttpRequest,
                               categoryKey: NepClient.CategoryKey,
                               pageNumber: NepClient.PageNumber = 1) extends NepRequest with CategoryRequestDetail
