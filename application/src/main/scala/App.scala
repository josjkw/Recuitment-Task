import domain.services.BoardService
import gateway.BoardHttpServer
import infrastructure.configuration.AppConfiguration
import infrastructure.migrations.DatabaseMigration
import infrastructure.repositories.PostgresLayers
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object App extends ZIOAppDefault {
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    for {
      _ <- DatabaseMigration.run.provide(AppConfiguration.live)
      _ <- BoardHttpServer.run.provide(
        BoardService.make,
        PostgresLayers.liveConnection,
        PostgresLayers.liveDataSource,
        PostgresLayers.liveDatabase,
        AppConfiguration.live
      )
    } yield ()

  }
}
