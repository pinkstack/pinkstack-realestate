import sbt._

object Dependencies {

  object V {
    val akka = "2.6.6"
    val akkaHttp = "10.1.12"

    val cats = "2.0.0"

    val monocle = "2.0.3"

    val typesafeConfig = "1.4.0"
    val pureConfig = "0.12.3"

    val scalaUri = "2.2.2"

    val decline = "1.2.0"

    val jsoup = "1.13.1"
    val circe = "0.12.3"

    val akkaHttpCirce = "1.31.0"
    val logbackClassic = "1.2.3"
    val scalaLogging = "3.9.2"
    val akkaSlf4j = "2.6.6"

    val scalaTest = "3.2.0"
  }

  lazy val akka: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % V.akka,
    "com.typesafe.akka" %% "akka-stream" % V.akka,

    "com.typesafe.akka" %% "akka-cluster" % V.akka,
    "com.typesafe.akka" %% "akka-cluster-typed" % V.akka,

    "com.typesafe.akka" %% "akka-discovery" % V.akka,
    "com.typesafe.akka" %% "akka-cluster-tools" % V.akka
  )

  lazy val akkaHttp: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-http" % V.akkaHttp
  )

  lazy val cats: Seq[ModuleID] = Seq(
    "org.typelevel" %% "cats-core" % V.cats
  )

  lazy val monocle: Seq[ModuleID] = Seq(
    "com.github.julien-truffaut" %% "monocle-core" % V.monocle,
    "com.github.julien-truffaut" %% "monocle-macro" % V.monocle,
    "com.github.julien-truffaut" %% "monocle-law" % V.monocle % "test"
  )

  lazy val configurationLibs: Seq[ModuleID] = Seq(
    "com.typesafe" % "config" % V.typesafeConfig,
    "com.github.pureconfig" %% "pureconfig" % V.pureConfig
  )

  lazy val scalaUri: Seq[ModuleID] = Seq(
    "io.lemonlabs" %% "scala-uri" % V.scalaUri
  )

  lazy val decline: Seq[ModuleID] = Seq(
    "com.monovore" %% "decline" % V.decline
  )

  lazy val jsoup: Seq[ModuleID] = Seq(
    "org.jsoup" % "jsoup" % V.jsoup
  )

  lazy val circe: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-core" % V.circe,
    "io.circe" %% "circe-generic" % V.circe,
    "io.circe" %% "circe-parser" % V.circe
  )

  lazy val akkaHttpCirce: Seq[ModuleID] = Seq(
    "de.heikoseeberger" %% "akka-http-circe" % V.akkaHttpCirce
  )

  lazy val logging: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic" % V.logbackClassic,
    "com.typesafe.scala-logging" %% "scala-logging" % V.scalaLogging,
    "com.typesafe.akka" %% "akka-slf4j" % V.akkaSlf4j
  )

  lazy val scalaTest: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % V.scalaTest % "test",
    "org.scalatest" %% "scalatest-flatspec" % V.scalaTest % "test",
    "org.scalatest" %% "scalatest-shouldmatchers" % V.scalaTest % "test"
  )

  lazy val javaAgentsLibs: Seq[ModuleID] = Seq(
    "io.kamon" % "kanela-agent" % "1.0.5"
  )
}
