package com.pinkstack.realestate

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ThrottleMode
import akka.stream.scaladsl.{Flow, Merge, Source}
import cats.data.NonEmptyChain
import cats.data.Validated.{Invalid, Valid}
import com.typesafe.scalalogging.LazyLogging
import io.lemonlabs.uri.typesafe.dsl._
import io.lemonlabs.uri.{AbsoluteUrl, Url}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContextExecutor, Future}

final case class NepClient(timeout: FiniteDuration = 4.seconds)(
  implicit val system: ActorSystem,
  val context: ExecutionContextExecutor,
  val configuration: Configuration) extends LazyLogging {

  import Domain._

  final val ROOT_URL = Url.parse("https://www.nepremicnine.net")

  val initialCategories: List[CategoryRequest] =
    configuration.seed.initialCategories
      .map(s => s"oglasi-$s")
      .map(c => CategoryRequest(HttpRequest(uri = ROOT_URL / c / '/' ? ("s" -> 16)), slug = c))

  val requestDocument: HttpRequest => Future[Document] = request =>
    for {
      request <- Http().singleRequest(request)
      pageBody <- request.entity.toStrict(timeout).map(_.data.utf8String)
    } yield Jsoup.parse(pageBody)

  val fetchCategoryPage: CategoryRequest => Future[(Option[List[CategoryRequest]], List[EstateRequest])] = { categoryRequest =>
    val lastCategoryPage: Document => Option[Int] = document =>
      for {
        lastHref <- Option(document.selectFirst("li.paging_last a.last")).map(_.attr("href"))
        lastPage <- """/(\d+)/""".r.findAllIn(lastHref).subgroups.headOption.map(_.toInt)
      } yield lastPage

    val hardLimit: Int => Int = lastPage => {
      val categoryPagesLimit = configuration.pagination.categoryPagesLimit
      if (lastPage >= categoryPagesLimit && categoryPagesLimit != -1) categoryPagesLimit else lastPage
    }

    val pageNumberToRequest: Int => CategoryRequest = page =>
      CategoryRequest(
        HttpRequest(uri = ROOT_URL / categoryRequest.slug / page / '/' ? ("s" -> 16)),
        slug = categoryRequest.slug)

    val categoryRequests: Document => Option[List[CategoryRequest]] = document =>
      for {
        lastPage <- lastCategoryPage(document)
        range = Range.inclusive(2, hardLimit(lastPage)).map(pageNumberToRequest).toList
      } yield range

    val estateRequests: Document => List[EstateRequest] = { document =>
      import scala.jdk.CollectionConverters._
      document.select("h2[itemprop=name] a").asScala
        .map(_.attr("href"))
        .map(s => AbsoluteUrl.parse(ROOT_URL.toString + s))
        .map(url => EstateRequest(HttpRequest(uri = url), categorySlug = categoryRequest.slug))
        .toList
    }

    requestDocument(categoryRequest.request)
      .map(doc => (categoryRequests(doc), estateRequests(doc)))
  }

  val fetchEstatePage: EstateRequest => Future[Option[Estate]] = { estateRequest =>
    val parse: Document => Option[Estate] = { document =>
      NepEstateParser.parse(document) match {
        case Valid(value: Estate) =>
          Some(value.copy(scrapedFromUrl = estateRequest.request.uri.toString))
        case Invalid(errors: NonEmptyChain[ParserValidation]) =>
          logger.error(s"${errors} at ${estateRequest.request.uri}")
          None
      }
    }

    requestDocument(estateRequest.request).map(parse)
  }

  val categoriesSource: Source[CategoryRequest, NotUsed] =
    Source(initialCategories)

  val pipeline: Source[Option[Estate], NotUsed] = {
    val (categoriesParallelism: Int, estatesParallelism: Int,
    elements: Int, per: Int, maximumBurst: Int) =
      (configuration.fetching.categoriesParallelism,
        configuration.fetching.estatesParallelism,
        configuration.throttlingEstates.elements,
        configuration.throttlingEstates.per,
        configuration.throttlingEstates.maximumBurst)

    val categoryFetch = Flow[CategoryRequest]
      .mapAsyncUnordered(categoriesParallelism)(fetchCategoryPage)

    val secondStage = Flow[(Option[List[CategoryRequest]], List[EstateRequest])]
      .flatMapConcat { case (cr, lr) =>
        val firstEstates = Source(lr).map(fetchEstatePage)

        val categoryEstates = Source(cr.getOrElse(List.empty[CategoryRequest]))
          .via(categoryFetch)
          .flatMapConcat { case (_, lx) => Source(lx).map(fetchEstatePage) }

        Source.combine(firstEstates, categoryEstates)(Merge(_))
      }

    categoriesSource
      .log(name = "categoriesSource")
      .via(categoryFetch)
      .via(secondStage)
      .throttle(elements, per.second, maximumBurst, ThrottleMode.Shaping)
      .mapAsyncUnordered(estatesParallelism)(identity)
  }
}
