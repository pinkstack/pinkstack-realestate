import java.io.InputStreamReader
import java.nio.file.Path

import cats._
import cats.implicits._
import com.pinkstack.realestate.Domain.Estate
import com.pinkstack.realestate.{NepEstateParser, NoTitleFound, NotActive}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.{EitherValues, PartialFunctionValues}
import org.scalatest.flatspec._
import org.scalatest.matchers.should.{Matchers => ShouldMatchers}

import scala.io.Source
import scala.util.{Try, Using}

class NepEstateParserSpec extends AnyFlatSpec
  with EitherValues
  with PartialFunctionValues
  with ShouldMatchers
  /* with ShouldMatchers */ {
  val read: String => Option[Document] = { name =>
    val getSource: String => Option[String] = name =>
      Using(Source.fromURL(getClass.getResource(name)))(_.mkString).toOption
    getSource(name).map(Jsoup.parse)
  }

  "An NepEstateParser" should "throw an error" in {
    read("404-error-page.html").map { doc =>
      val result = NepEstateParser.parse(doc)
      assert(result.left.value == NotActive)
    }
  }

  it should "regular listing" in {
    read("dravograd-stanovanje.html").map { doc =>
      NepEstateParser.parse(doc) match {
        case Right(Estate(title, price, refNumber, sourceUri, categories, locationDetails)) =>
          assert(title == "Prodaja, Stanovanje, 4-sobno: DRAVOGRAD, 146.2 m2")
          assert(categories("Posredovanje") == "Prodaja")
          assert(refNumber == "6305223")
        case Left(value) => fail(s"Invalid validation state $value")
      }
    }
  }

  it should "work on big numbers" in {
    val estates: List[(String, Double)] = List(
      ("dravograd-stanovanje.html", 95000.0),
      ("novigrad-hisa.html", 1000085.0)
    )

    estates.map(e => (e._1, e._2, read(e._1))).foreach {
      case (name, expectedPrice, Some(document)) =>
        NepEstateParser.parse(document) match {
          case Right(estate: Estate) =>
            assert(estate.price == expectedPrice)
          case Left(value) =>
            fail(s"Failed with $value at $name")
        }
      case _ =>
        fail(s"Can't read document.")
    }
  }
}
