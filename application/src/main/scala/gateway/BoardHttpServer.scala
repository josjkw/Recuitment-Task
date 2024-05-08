package gateway

import domain.services.BoardService
import infrastructure.configuration.AppConfiguration
import io.github.gaelrenoux.tranzactio.doobie.{Connection, Database}
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import sttp.apispec.openapi.Info
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import zio.interop.catz._
import zio.{RIO, ZIO, ZLayer}

object BoardHttpServer {

  type Env = Database with Connection with BoardService

  type AppTask[A] = RIO[Env, A]

  private val allEndpoints                = BoardRoutes.boardEndpoints
  private val routes: HttpRoutes[AppTask] = ZHttp4sServerInterpreter().from(allEndpoints).toRoutes

  val swaggerEndpoints = SwaggerInterpreter().fromServerEndpoints[AppTask](allEndpoints, Info("Api Chess", "1.0"))
  val swaggerRoutes: HttpRoutes[AppTask] = ZHttp4sServerInterpreter().from(swaggerEndpoints).toRoutes

  private val httpApp = Router[AppTask](
    "/" -> routes,
    "/" -> swaggerRoutes
  ).orNotFound

  val run: ZIO[Env with AppConfiguration, Throwable, Unit] = for {
    config <- ZIO.serviceWith[AppConfiguration](_.httpServer)
    _ <- BlazeServerBuilder[AppTask]
      .bindHttp(config.port, config.host)
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
  } yield ()

  val make = ZLayer.fromZIO(run)

}
