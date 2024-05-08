package infrastructure.configuration
import pureconfig.generic.auto._
import pureconfig.{ConfigObjectSource, ConfigSource}
import zio.{ZIO, ZLayer}

final case class KafkaProducer(address: String, client: String, topic: String)
final case class Postgres(address: String, user: String, password: String)
final case class HttpServer(port: Int, host: String)

final case class AppConfiguration(
  kafkaProducer: KafkaProducer,
  postgres: Postgres,
  httpServer: HttpServer
)

object AppConfiguration {

  val live: ZLayer[Any, Throwable, AppConfiguration] = ZLayer.fromZIO {
    ZIO.attempt {
      val configSource: ConfigObjectSource = ConfigSource.default
      configSource.loadOrThrow[AppConfiguration]
    }
  }

}
