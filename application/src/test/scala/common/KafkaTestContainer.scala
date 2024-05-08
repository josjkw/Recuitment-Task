package common

import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import zio.ZIO.attemptBlocking
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZSink
import zio.{Scope, ZIO, ZLayer}

object KafkaTestContainer {

  final case class KafkaContainerConfig(address: String)

  private val kafkaImage = "confluentinc/cp-kafka:7.3.0"

  private val kafkaGroup  = "group"
  private val kafkaClient = "client"
  private val kafkaTopic  = "events"

  private def consumerSettings(kafkaAddress: String): ConsumerSettings =
    ConsumerSettings(List(kafkaAddress))
      .withGroupId(kafkaGroup)
      .withClientId(kafkaClient)

  private val subscription = Subscription.topics(kafkaTopic)

  private def consumerLayer(consumerSettings: ConsumerSettings) = ZLayer.scoped(Consumer.make(consumerSettings))

  def runConsume(kafkaAddress: String, numberOfEvents: Int): ZIO[Any, Throwable, Set[String]] =
    for {
      events <- Consumer
        .plainStream(
          subscription,
          Serde.string,
          Serde.string
        )
        .tap { record => ZIO.log(s"Received ${record.key}: ${record.value}") }
        .mapZIO { r =>
          r.offset.commit.as(r.value)
        }
        .provideLayer(consumerLayer(consumerSettings(kafkaAddress)))
        .run(ZSink.take[String](numberOfEvents).ignoreLeftover.map(_.toSet))
        .orDie

    } yield events

  val live: ZLayer[Scope, Nothing, KafkaContainerConfig] = ZLayer.fromZIO {
    ZIO
      .acquireRelease {
        ZIO.attemptBlocking {
          val kafkaContainer =
            new KafkaContainer(DockerImageName.parse(kafkaImage))

          kafkaContainer.start()
          kafkaContainer
        }
      }(c => ZIO.succeed(c.stop()))
      .flatMap(container =>
        attemptBlocking {
          KafkaContainerConfig(container.getBootstrapServers)
        }
      )
  }.orDie

}
