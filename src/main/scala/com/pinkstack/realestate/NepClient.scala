package com.pinkstack.realestate

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ThrottleMode
import akka.stream.scaladsl.{Flow, Merge, Source}
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
      .map(c => new CategoryRequest(HttpRequest(uri = ROOT_URL / c / '/' ? ("s" -> 16)), slug = c))
      .toList

  val categoriesSource: Source[CategoryRequest, NotUsed] =
    Source(initialCategories)

  val requestDocument: HttpRequest => Future[Document] = request =>
    for {
      request <- Http().singleRequest(request)
      pageBody <- request.entity.toStrict(timeout).map(_.data.utf8String)
      document <- Future.successful(Jsoup.parse(pageBody))
    } yield document

  val fetchCategoryPage: CategoryRequest => Future[(Option[List[CategoryRequest]], List[EstateRequest])] = { categoryRequest =>
    val lastCategoryPage: Document => Option[Int] = { document =>
      Option(document.selectFirst("li.paging_last a.last"))
        .map(_.attr("href"))
        .flatMap { href => """/(\d+)/""".r.findAllIn(href).subgroups.headOption.map(_.toInt) }
    }
    val hardLimit: Int => Int = lastPage =>
      if (lastPage >= configuration.pagination.categoryPagesLimit)
        configuration.pagination.categoryPagesLimit else lastPage

    val pageNumberToRequest: Int => CategoryRequest = page => {
      new CategoryRequest(
        HttpRequest(uri = ROOT_URL / categoryRequest.slug / page / '/' ? ("s" -> 16)),
        slug = categoryRequest.slug)
    }

    val categoryRequests: Document => Option[List[CategoryRequest]] = { document =>
      lastCategoryPage(document).flatMap(n => Option(Range(2, hardLimit(n)).map(pageNumberToRequest)).map(_.toList))
    }

    val estateRequests: Document => List[EstateRequest] = { document =>
      import scala.jdk.CollectionConverters._
      document.select("h2[itemprop=name] a").asScala
        .map(_.attr("href"))
        .map(s => AbsoluteUrl.parse(ROOT_URL.toString + s))
        .map(url => new EstateRequest(HttpRequest(uri = url), categorySlug = categoryRequest.slug))
        .toList
    }

    requestDocument(categoryRequest.request)
      .map(doc => (categoryRequests(doc), estateRequests(doc)))
  }

  val fetchEstatePage: EstateRequest => Future[Option[Estate]] = { estateRequest =>
    val parse: Document => Option[Estate] = doc =>
      for {
        first <- Option(doc.selectFirst("title"))
        title <- first.text().split(""" - Nep""").toList.headOption.map(_.strip())
      } yield Estate(estateRequest.request.uri, title)

    requestDocument(estateRequest.request).map(parse)
  }

  val pipeline: Source[Option[Estate], NotUsed] = {
    val (categoriesParallelism: Int, estatesParallelism: Int) =
      (configuration.fetching.categoriesParallelism,
        configuration.fetching.estatesParallelism)

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
      .throttle(20, 1.second, 30, ThrottleMode.Shaping)
      .mapAsyncUnordered(estatesParallelism)(identity)
  }
}
