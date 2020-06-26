package com.pinkstack.realestate

import com.pinkstack.realestate.Domain._
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
  val validatedTitle: Document => Either[ParserValidation, String] = {
    document: Document =>
      val title: Document => Option[String] = document => for {
        titleRaw <- Option(document.selectFirst("title")).map(_.text)
        firstPart <- titleRaw.split("""(-|\|) Nep""").headOption.map(_.strip)
      } yield firstPart

      title(document).fold[Either[ParserValidation, String]](Left(NoTitleFound))(Right(_))
  }

  val validatedVisibility: String => Either[ParserValidation, Boolean] = { title: String =>
    if (title.contains("ne obstaja")) Left(NotActive) else Right(true)
  }

  val validatedCategories: Document => Either[ParserValidation, Map[String, String]] = { document =>
    Option(document.selectFirst("div.more_info"))
      .map(_.text())
      .map(_.split(""" \| """)
        .toList.map(_.strip)
        .map(_.split(""":""").toList.map(_.strip))
        .foldLeft(collection.mutable.Map.empty[String, String]) {
          (agg, v) => agg(v.head) = v.last; agg
        }
        .toMap)
    match {
      case Some(map) => Right(map)
      case None => Left(NoCategories)
    }
  }

  val validatedPrice: Document => Either[ParserValidation, Double] = document =>
    document.selectFirst("meta[itemprop='price']")
      .attr("content")
      .toDoubleOption match {
      case Some(value: Double) => Right(value)
      case None => Left(NoPrice)
    }

  val validatedRefNumber: Document => Either[ParserValidation, String] = document =>
    Option(document.selectFirst("span.ikona-sh"))
      .map(_.attr("data-id")) match {
      case Some(value) => Right(value)
      case None => Left(NoRefNumber)
    }

  val validatedLocationDetails: Document => Either[ParserValidation, Map[String, Vector[String]]] = { document =>
    import scala.jdk.CollectionConverters._

    document.getElementsByTag("script")
      .asScala.toList.map(_.html).find(_.contains("#mapFrame"))
      .flatMap {
        """src="(.*)"""".r.findAllIn(_).subgroups.headOption
          .map(Url.parse(_))
          .map(_.query.paramMap.toMap)
      } match {
      case Some(value) => Right(value)
      case None => Left(NoLocationFound)
    }
  }

  val parse: Document => Either[ParserValidation, Estate] = { document: Document =>
    validatedTitle(document).flatMap { title: String =>
      validatedVisibility(title).flatMap { _ =>
        validatedRefNumber(document).flatMap { refNumber =>
          validatedLocationDetails(document).flatMap { locationDetails =>
            validatedPrice(document).flatMap { price =>
              validatedCategories(document).map { categories =>
                Estate(title, price, refNumber, categories = categories, locationDetails = locationDetails)
              }
            }
          }
        }
      }
    }
  }
}