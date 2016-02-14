name := """play-with-websockets"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  ws,
  "org.java-websocket" % "Java-WebSocket" % "1.3.0",
  "org.specs2" %% "specs2-core" % "3.7" % "test"
)

routesGenerator := InjectedRoutesGenerator
