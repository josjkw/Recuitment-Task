import sbt.*
import sbt.Keys.libraryDependencies
import sbt.librarymanagement.ModuleID

object Dependencies {

  private val config = Seq(
    "com.typesafe"           % "config"     % "1.4.3",
    "com.github.pureconfig" %% "pureconfig" % "0.17.6"
  )

  lazy val zio = Seq(
    "dev.zio" %% "zio"       % "2.0.21",
    "dev.zio" %% "zio-kafka" % "2.7.4"
  )

  lazy val zioTest = Seq(
    "dev.zio" %% "zio-test"          % "2.0.21" % Test,
    "dev.zio" %% "zio-test-sbt"      % "2.0.22" % Test,
    "dev.zio" %% "zio-test-magnolia" % "2.0.22" % Test
  )

  lazy val doobie = Seq(
    "org.tpolecat" %% "doobie-core"     % "1.0.0-RC2",
    "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC2"
  )

  lazy val tzio = Seq("io.github.gaelrenoux" %% "tranzactio" % "4.0.0")

  lazy val tapir = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server"   % "1.10.0",
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % "1.10.0",
    "com.softwaremill.sttp.tapir" %% "tapir-http4s-server-zio" % "1.10.0",
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "1.10.0"
  )

  lazy val circe = Seq(
    "io.circe" %% "circe-core"           % "0.14.7",
    "io.circe" %% "circe-generic"        % "0.14.7",
    "io.circe" %% "circe-parser"         % "0.14.7",
    "io.circe" %% "circe-generic-extras" % "0.14.3"
  )

  lazy val logback = Seq(
    "org.slf4j"      % "slf4j-api"       % "2.0.12",
    "ch.qos.logback" % "logback-classic" % "1.5.6"
  )

  lazy val flyway = Seq(
    "org.flywaydb" % "flyway-core" % "8.0.0"
  )

  lazy val chimney = Seq(
    "io.scalaland" %% "chimney" % "0.8.5"
  )

  lazy val http4s = Seq(
    "org.http4s" %% "http4s-blaze-server" % "0.23.13",
    "org.http4s" %% "http4s-blaze-client" % "0.23.13" % Test,
    "org.http4s" %% "http4s-circe"        % "0.23.13" % Test
  )

  lazy val enumeratum = Seq(
    "com.beachape" %% "enumeratum"       % "1.7.2",
    "com.beachape" %% "enumeratum-circe" % "1.7.2"
  )

  lazy val testContainer = Seq(
    "org.testcontainers" % "kafka"          % "1.19.7" % Test,
    "org.testcontainers" % "testcontainers" % "1.19.7" % Test,
    "org.testcontainers" % "postgresql"     % "1.19.7" % Test
  )

  val application =
    zio ++ tapir ++ doobie ++ tzio ++ circe ++ logback ++
      flyway ++ chimney ++ http4s ++ zioTest ++ config ++ enumeratum ++ testContainer

  val client = config ++ zio
}
