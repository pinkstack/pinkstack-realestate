name := "pinkstack-realestate"

version := "0.0.1"

scalaVersion := "2.13.2"

libraryDependencies ++= Seq(
  // Akka and Akka Streams
  "com.typesafe.akka" %% "akka-stream" % "2.6.6",
  "com.typesafe.akka" %% "akka-http" % "10.1.12",

  // FP
  "org.typelevel" %% "cats-core" % "2.0.0",

  // Configuration
  "com.typesafe" % "config" % "1.4.0",

  // URL Parsing
  "io.lemonlabs" %% "scala-uri" % "2.2.2",

  // Parsing
  "org.jsoup" % "jsoup" % "1.13.1",

  // Logging
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "com.typesafe.akka" %% "akka-slf4j" % "2.6.6",
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-target:jvm-1.11",
  "-unchecked"
)