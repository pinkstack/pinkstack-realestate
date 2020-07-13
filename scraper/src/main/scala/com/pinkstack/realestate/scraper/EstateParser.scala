package com.pinkstack.realestate.scraper

import cats._
import cats.data.{Validated, ValidatedNec}
import cats.implicits._
import Domain._
import io.lemonlabs.uri.Url
import org.jsoup.nodes.Document

sealed trait ParserValidation {
  def errorMessage: String
}

case object NoTitleFound extends ParserValidation {
  override def errorMessage: String = "No title found."
}

case object NotActive extends ParserValidation {
  override def errorMessage: String = "Estate ad is not available."
}

case object NoCategories extends ParserValidation {
  override def errorMessage: String = "No categories found."
}

case object NoPrice extends ParserValidation {
  override def errorMessage: String = "No price was found."
}

case object NoRefNumber extends ParserValidation {
  override def errorMessage: String = "No refNumber was found."
}

case object NoLocationFound extends ParserValidation {
  override def errorMessage: String = "No location was found."
}

object NepEstateParser {
  type ValidationResult[A] = ValidatedNec[ParserValidation, A]
  type Transformer[A] = Document => ValidationResult[A]

  val validatedTitle: Transformer[String] = { document: Document =>
    val title: Document => Option[String] = document => for {
      titleRaw <- Option(document.selectFirst("title")).map(_.text)
      firstPart <- titleRaw.split("""(-|\|) Nep""").headOption.map(_.strip)
    } yield firstPart

    title(document).fold[ValidationResult[String]](NoTitleFound.invalidNec)(s => s.validNec)
  }

  val validatedVisibility: String => ValidationResult[Boolean] = title =>
    Option.when(!title.contains("ne obstaja"))(true)
      .fold[ValidatedNec[ParserValidation, Boolean]](NotActive.invalidNec)(_.validNec)

  val validatedCategories: Transformer[Map[String, String]] = { document =>
    val parseCategories: String => Map[String, String] = _.split(""" \| """)
      .toList.map(_.strip).map(_.split(""":""").toList.map(_.strip))
      .foldLeft(collection.mutable.Map.empty[String, String]) {
        (agg, v) => agg(v.head) = v.last; agg
      }.toMap
    val categoriesOpt = Option(document.selectFirst("div.more_info"))
      .map(_.text())
      .map(parseCategories)

    categoriesOpt.fold[ValidationResult[Map[String, String]]](NoCategories.invalidNec)(_.validNec)
  }

  val validatedPrice: Transformer[Double] = document =>
    Option(document.selectFirst("meta[itemprop='price']"))
      .flatMap(_.attr("content").toDoubleOption)
      .fold[ValidationResult[Double]](NoPrice.invalidNec)(_.validNec)

  val validatedRefNumber: Transformer[String] = document =>
    Option(document.selectFirst("span.ikona-sh"))
      .map(_.attr("data-id"))
      .fold[ValidationResult[String]](NoRefNumber.invalidNec)(_.validNec)

  val validatedLocationDetails: Transformer[Map[String, Vector[String]]] = { document =>
    import scala.jdk.CollectionConverters._
    val params = for {
      frame <- document.getElementsByTag("script").asScala.toList.map(_.html).find(_.contains("#mapFrame"))
      p <-
        """src="(.*)"""".r.findAllIn(frame).subgroups.headOption
          .map(Url.parse(_))
          .map(_.query.paramMap.toMap)
    } yield p

    params.fold[ValidationResult[Map[String, Vector[String]]]](NoLocationFound.invalidNec)(_.validNec)
  }

  val locationFromDetails: Map[String, Vector[String]] => Option[Location] = { details =>
    for {
      coordsStr <- details.get("coord").flatMap(_.headOption)
      matching <- """(-?\d+(\.\d+)?),\s*(-?\d+(\.\d+)?)""".r.findFirstIn(coordsStr).map(_.split(",", 2).map(_.toDouble))
    } yield {
      val Array(x, y) = matching
      Location(x, y)
    }
  }

  val parse: Document => ValidationResult[Estate] = { document: Document =>
    def buildEstate(title: String, refNumber: String, price: Double, visibility: Boolean,
                    locationDetails: Map[String, Vector[String]],
                    categories: Map[String, String]): Estate =
      Estate(title, refNumber, price, visibility, "",
        locationDetails, categories, locationFromDetails(locationDetails))

    (
      validatedTitle(document),
      validatedRefNumber(document),
      validatedPrice(document),
      validatedTitle(document).withEither(_.flatMap(title => validatedVisibility(title).toEither)),
      validatedLocationDetails(document),
      validatedCategories(document),
      ).mapN(buildEstate)
  }
}