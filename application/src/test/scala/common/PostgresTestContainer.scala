package common

import org.testcontainers.containers.PostgreSQLContainer
import zio.ZIO.attemptBlocking
import zio.{Scope, ZIO, ZLayer}

object PostgresTestContainer {

  final case class PostgresContainerConfig(address: String, user: String, password: String)

  private val postgresImage = "postgres:11-alpine"
  private val databaseName  = "chess"
  private val user          = "root"
  private val password      = "notlongenoughpassword"

  val live: ZLayer[Scope, Nothing, PostgresContainerConfig] = ZLayer.fromZIO {
    ZIO
      .acquireRelease {
        ZIO.attemptBlocking {
          val postgresContainer =
            new PostgreSQLContainer(postgresImage)

          postgresContainer.withDatabaseName(databaseName)
          postgresContainer.withUsername(user)
          postgresContainer.withPassword(password)
          postgresContainer.start()
          postgresContainer
        }
      }(c => ZIO.succeed(c.stop()))
      .flatMap(container =>
        attemptBlocking {
          PostgresContainerConfig(container.getJdbcUrl, user, password)
        }
      )
  }.orDie

}
