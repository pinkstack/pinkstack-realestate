name := "pinkstack-realestate"

version := "0.0.1"

scalaVersion := "2.13.2"

libraryDependencies ++= Seq(
  // Akka and Akka Streams
  "com.typesafe.akka" %% "akka-http" % "10.1.12",
  "com.typesafe.akka" %% "akka-actor-typed" % "2.6.6",
  "com.typesafe.akka" %% "akka-stream" % "2.6.6",

  // Clustering and remoting
  "com.typesafe.akka" %% "akka-cluster" % "2.6.6",
  "com.typesafe.akka" %% "akka-cluster-typed" % "2.6.6",
  // "com.lightbend.akka.management" %% "akka-management" % "1.0.8",
  // "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % "1.0.8",
  "com.typesafe.akka" %% "akka-discovery" % "2.6.6",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.6.6",

  // FP
  "org.typelevel" %% "cats-core" % "2.0.0",

  // Lenses
  "com.github.julien-truffaut" %% "monocle-core" % "2.0.3",
  "com.github.julien-truffaut" %% "monocle-macro" % "2.0.3",
  "com.github.julien-truffaut" %% "monocle-law" % "2.0.3" % "test",

  // Configuration
  "com.typesafe" % "config" % "1.4.0",
  "com.github.pureconfig" %% "pureconfig" % "0.12.3",

  // URL Parsing
  "io.lemonlabs" %% "scala-uri" % "2.2.2",

  // CLI
  "com.monovore" %% "decline" % "1.2.0",

  // Parsing
  "org.jsoup" % "jsoup" % "1.13.1",

  // JSON
  "io.circe" %% "circe-core" % "0.12.3",
  "io.circe" %% "circe-generic" % "0.12.3",
  "io.circe" %% "circe-parser" % "0.12.3",

  // JSON + Akka
  "de.heikoseeberger" %% "akka-http-circe" % "1.31.0",

  // Logging
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "com.typesafe.akka" %% "akka-slf4j" % "2.6.6",

  // Testing
  "org.scalatest" %% "scalatest" % "3.2.0" % "test",
  "org.scalatest" %% "scalatest-flatspec" % "3.2.0" % "test",
  "org.scalatest" %% "scalatest-shouldmatchers" % "3.2.0" % "test"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-target:jvm-1.11",
  "-unchecked"
)

enablePlugins(JavaAppPackaging)

mainClass in assembly := Some("com.pinkstack.realestate.apps.Scraper")
assemblyJarName in assembly := "scraper.jar"