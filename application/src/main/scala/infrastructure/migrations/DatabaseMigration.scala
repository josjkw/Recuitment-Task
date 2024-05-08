package infrastructure.migrations

import infrastructure.configuration.AppConfiguration
import org.flywaydb.core.Flyway
import zio.{ZIO, ZLayer}

object DatabaseMigration {

  private def migrateDatabase(url: String, user: String, password: String): Unit = {
    Flyway
      .configure()
      .dataSource(url, user, password)
      .locations("classpath:db/migrations")
      .load()
      .migrate()

  }

  def run = for {
    config <- ZIO.serviceWith[AppConfiguration](_.postgres)
    _ <- ZIO.attemptBlocking {
      migrateDatabase(
        config.address,
        config.user,
        config.password
      )
    }
  } yield ()

  def live: ZLayer[AppConfiguration, Throwable, Unit] = ZLayer.fromZIO(run)

}
