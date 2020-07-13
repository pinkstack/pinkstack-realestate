package com.pinkstack.realestate.apps

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import cats.data._
import cats.implicits._
import com.monovore.decline._
import com.pinkstack.realestate.Domain.Estate
import com.pinkstack.realestate.{Configuration, NepClient}
import io.circe.generic.auto._
import io.circe.syntax._
import monocle.macros.GenLens

import scala.concurrent.ExecutionContextExecutor

object Implicits {
  type CategoryList = List[String]

  implicit val categoriesArgument: Argument[CategoryList] = new Argument[CategoryList] {
    final val POSSIBLE_CATEGORIES: List[String] = List("prodaja", "nakup", "oddaja", "najem")

    override def read(string: String): ValidatedNel[String, List[String]] = {
      string.split(",|\\s").toList.map { c =>
        Option.when(POSSIBLE_CATEGORIES.contains(c))(c)
          .fold[Either[String, String]](s"""Unknown category "$c".""".asLeft)(_.asRight)
      }.traverse(Validated.fromEither).toValidatedNel
    }

    def defaultMetavar = "prodaja,nakup,oddaja,najem"
  }
}

import Implicits._

object Main {

  import Implicits._

  val estatePrinter: Option[Estate] => Unit = {
    case Some(estate: Estate) =>
      println(estate.asJson.noSpaces)
    case None =>
  }

  def withConfiguration(categories: CategoryList, numberOfPages: Int): Unit = {
    val configurationLens: Configuration => Configuration =
      GenLens[Configuration](_.seed.initialCategories).set(categories) compose
        GenLens[Configuration](_.pagination.categoryPagesLimit).set(numberOfPages)

    implicit val configuration: Configuration = configurationLens(Configuration.loadOrThrow)
    implicit val system: ActorSystem = ActorSystem("scraper")
    implicit val context: ExecutionContextExecutor = system.dispatcher

    NepClient()
      .pipeline
      .runWith(Sink.foreach(estatePrinter))
      .onComplete(_ => system.terminate())
  }
}

object Scraper extends CommandApp(name = "scraper",
  header = "Scraper is used for fetching real estate listings directly in CLI.",
  main = {
    val categoriesOpt = Opts.option[CategoryList]("categories", "List of possible categories to fetch.")
    val numberOfPagesOpt = Opts.option[Int]("pages", "Number of pages to fetch from each category")
      .withDefault(3)

    (categoriesOpt, numberOfPagesOpt).mapN(Main.withConfiguration)
  }
)
