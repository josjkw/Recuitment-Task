package infrastructure

import common.KafkaTestContainer
import common.KafkaTestContainer.KafkaContainerConfig
import domain.repositories.Event
import infrastructure.configuration.{AppConfiguration, HttpServer, KafkaProducer, Postgres}
import infrastructure.repositories.KafkaEventRepository
import zio.test.TestAspect.samples
import zio.test._
import zio.{Scope, ZIO, ZLayer}

object KafkaRepositorySpec extends ZIOSpecDefault {

  val appConfigTest: ZLayer[KafkaContainerConfig, Nothing, AppConfiguration] = ZLayer.fromZIO {
    ZIO.serviceWith[KafkaContainerConfig] { kafkaAddress =>
      AppConfiguration(
        kafkaProducer = KafkaProducer(
          address = kafkaAddress.address,
          client = "client",
          topic = "events"
        ),
        postgres = Postgres(
          address = "foo",
          user = "foo",
          password = "foo"
        ),
        httpServer = HttpServer(
          port = 1234,
          host = "foo"
        )
      )
    }
  }
  val kafkaEventRepositoryLayer = appConfigTest >>> KafkaEventRepository.live

  val shouldEmitMessages = test("should emit messages") {
    check(Gen.setOf1(Gen.string)) { eventsValue =>
      for {
        config          <- ZIO.service[KafkaContainerConfig]
        kafkaRepository <- ZIO.service[KafkaEventRepository]
        recordsFib      <- KafkaTestContainer.runConsume(config.address, eventsValue.size).fork
        _ <- ZIO.succeed(Thread.sleep(2000L)) // workaround, wait for consumer to be fully operational
        _ <- kafkaRepository.run().fork
        _ <- ZIO.foreachDiscard(eventsValue) { v =>
          kafkaRepository.produceEvent(Event(v))
        }
        records <- recordsFib.join
      } yield assertTrue(records == eventsValue)
    }

  } @@ samples(3)

  val kafkaSuite = suite("Kafka")(shouldEmitMessages)

  override def spec: Spec[TestEnvironment with Scope, Any] =
    kafkaSuite.provideShared(kafkaEventRepositoryLayer, KafkaTestContainer.live, Scope.default)
}
