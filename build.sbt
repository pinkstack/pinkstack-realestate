import Dependencies._
import Settings._
import scala.sys.process._

lazy val scraper = (project in file("scraper"))
  .settings(sharedSettings: _*)
  .settings(
    name := "scraper",
    Compile / mainClass := Some(""),
    libraryDependencies ++=
      akka ++ akkaHttp ++ cats
        ++ monocle ++ configurationLibs ++ scalaUri
        ++ decline ++ jsoup ++ circe
        ++ akkaHttpCirce ++ logging ++ scalaTest,
    Compile / mainClass := Some("com.pinkstack.realestate.scraper.apps.Scraper"),
  )
  .enablePlugins(JavaAppPackaging)
  .settings(assemblyJarName in assembly := "scraper.jar")

lazy val cluster = (project in file("cluster"))
  .settings(sharedSettings: _*)
  .settings(
    name := "cluster",
    Compile / mainClass := Some(""),
    libraryDependencies ++=
      akka ++ akkaHttp ++ cats
        ++ monocle ++ configurationLibs ++ scalaUri
        ++ decline ++ jsoup ++ circe
        ++ akkaHttpCirce ++ logging ++ scalaTest
  )
  .enablePlugins(JavaAppPackaging)
  .settings(assemblyJarName in assembly := "cluster.jar")
  .dependsOn(scraper)
