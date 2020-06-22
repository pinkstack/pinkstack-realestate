package com.pinkstack.realestate

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import com.typesafe.scalalogging.LazyLogging
import io.lemonlabs.uri.{AbsoluteUrl, Url}
import io.lemonlabs.uri.typesafe.dsl._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.concurrent.Future
import scala.concurrent.duration._

trait HttpClient {
  def singleRequest(request: HttpRequest, timeout: FiniteDuration = 2.seconds): Future[String]

  def requestAndParse(request: HttpRequest, timeout: FiniteDuration = 2.seconds): Future[Document]
}

trait NepClient {
  this: AkkaApp with HttpClient with LazyLogging =>

  type CategoryKey = String
  type PageNumber = Int

  // implicit val system: ActorSystem
  // implicit val executionContext: ExecutionContext

  final val ROOT_URL: Url = Url.parse("https://www.nepremicnine.net")

  implicit val lemonLabsUrlToAkkaUrl: Url => Uri = { lemonUrl: Url => Uri(lemonUrl.toString()) }

  def singleRequest(request: HttpRequest, timeout: FiniteDuration = 2.seconds): Future[String] = {
    for {
      request <- Http().singleRequest(request)
      pageBody <- request.entity.toStrict(timeout).map(_.data.utf8String)
    } yield pageBody
  }

  def requestAndParse(request: HttpRequest, timeout: FiniteDuration = 2.seconds): Future[Document] = {
    logger.debug(s"Requesting ${request}")
    for {
      pageBody <- singleRequest(request, timeout)
      document <- Future.successful(Jsoup.parse(pageBody))
    } yield document
  }

  def seedCategoryRequests: List[CategoryPageRequest] = {
    "prodaja,nakup,najem,oddaja".split(",")
      .map(s => s"oglasi-$s")
      .map(c => CategoryPageRequest(HttpRequest(uri = ROOT_URL / c / '/' ? ("s" -> 16)), c))
      .toList
  }

  def fetchCategoryRequests(nepCategoryRequest: CategoryPageRequest): Future[List[CategoryRequestDetail]] = {
    import scala.jdk.CollectionConverters._

    logger.debug(s"Fetching category ${nepCategoryRequest.categoryKey} with page ${nepCategoryRequest.pageNumber}")

    val lastCategoryPage: Document => Option[Int] = { document =>
      val lastPageHref = document.selectFirst("li.paging_last a.last").attr("href")
      """/(\d+)/""".r.findAllIn(lastPageHref).subgroups.headOption.map(_.toInt)
    }
    val hardLimit: Int => Int = { lastPage: Int => if (lastPage > 10) 10 else lastPage }

    val categoryRequests: Document => List[CategoryPageRequest] = { document =>
      val pageToRequest: Int => CategoryPageRequest = { pageNumber: Int =>
        CategoryPageRequest(HttpRequest(
          uri = ROOT_URL / nepCategoryRequest.categoryKey / pageNumber / '/' ? ("s" -> 16)),
          nepCategoryRequest.categoryKey, pageNumber)
      }

      lastCategoryPage(document) match {
        case Some(lastPage: Int) => Range(2, hardLimit(lastPage)).map(pageToRequest).toList
        case None => List.empty[CategoryPageRequest]
      }
    }

    val estateRequests: Document => List[EstateRequest] = { document =>
      document.select("h2[itemprop=name] a").asScala
        .map(_.attr("href"))
        .map(s => AbsoluteUrl.parse(ROOT_URL.toString() + s))
        .map(url => EstateRequest(HttpRequest(uri = url)))
        .toList
    }

    requestAndParse(nepCategoryRequest.request)
      .map(document => categoryRequests(document) ++ estateRequests(document))
  }

  def fetchEstateListing(nepEstateRequest: EstateRequest): Future[String] = {
    requestAndParse(nepEstateRequest.request, 1.seconds)
      .map(_.selectFirst("title").text())
  }

  def fetchWithinPage(categoryPageRequest: CategoryPageRequest): Future[List[String]] = {
    val f = for {
      details <- NepClient.fetchCategoryRequests(categoryPageRequest)
      estateRequests = details.filter(_.isInstanceOf[EstateRequest])
    } yield {
      estateRequests.map {
        case request: EstateRequest =>
          NepClient.fetchEstateListing(request)
        case _ =>
          Future.never
      }
    }
    f.flatMap(s => Future.sequence(s))
  }
}

object NepClient extends NepClient with AkkaApp with HttpClient with LazyLogging