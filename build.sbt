name := """play-spaceships"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.6"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws
)
