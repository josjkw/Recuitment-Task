import sbt.Keys.scalacOptions

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.13"

lazy val application = (project in file("application"))
  .settings(
    name := "application",
    libraryDependencies ++= Dependencies.application,
    scalacOptions += "-Wunused:imports"
  )

lazy val client = (project in file("client"))
  .settings(
    name := "client",
    libraryDependencies ++= Dependencies.client,
    scalacOptions += "-Wunused:imports"
  )

lazy val root = (project in file("."))
  .aggregate(application, client)
  .settings(
    name := "recruitment-task-beone",
    run := (application / Compile / run).evaluated,
    Global / cancelable := false
  )
