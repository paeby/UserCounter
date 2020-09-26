name := "UserCounter"

organization := "com.doodle"

version := "0.1"

scalaVersion := "2.12.12"

libraryDependencies ++=
  Seq(
    "org.apache.kafka"           % "kafka-clients"              % "2.6.0",
    "io.circe"                   %% "circe-generic"             % "0.12.3",
    "com.ovoenergy"              %% "kafka-serialization-core"  % "0.5.22",
    "com.ovoenergy"              %% "kafka-serialization-circe" % "0.5.22",
    "com.fasterxml.jackson.core" % "jackson-databind"           % "2.8.0",
    "com.typesafe"               % "config"                     % "1.3.0",
    // Test Dependencies
    "org.scalactic" %% "scalactic" % "3.0.5",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test"
  )

scalafmtConfig in ThisBuild := file("scalafmt.conf")
scalafmtOnCompile in ThisBuild := true

resolvers ++= Seq(
  "hortonworks-releases" at "http://repo.hortonworks.com/content/groups/public",
  "mvn" at "https://dl.bintray.com/ovotech/maven"
)

mainClass in (Compile, run) := Some("com.doodle.stream.UserCounterApp")
