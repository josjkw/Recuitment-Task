package infrastructure.repositories

import domain.repositories.{Event, EventRepository}
import infrastructure.configuration.AppConfiguration
import org.apache.kafka.clients.producer.ProducerRecord
import zio.kafka.producer._
import zio.kafka.serde._
import zio.stream.ZStream
import zio.{Clock, Queue, Scope, ZIO, ZLayer}

final case class KafkaTopic(value: String)
object KafkaTopic {
  def fromString(s: String): Either[String, KafkaTopic] =
    Either.cond(s.nonEmpty, KafkaTopic(s), "Kafka topic must be non empty")
}

class KafkaEventRepository(eventQueue: Queue[Event], producerSettings: ProducerSettings, topic: KafkaTopic)
    extends EventRepository {

  implicit val eventSerde: Serde[Any, Event] = Serde.string.inmap(s => Event(s))(e => e.event)

  private val producerLayer = ZLayer.fromZIO(Producer.make(producerSettings))

  private val kafkaStream = ZStream
    .fromQueue(eventQueue)
    .mapZIO(e => Clock.currentDateTime.map(_ -> e))
    .map { case (time, event) =>
      new ProducerRecord(topic.value, time.toEpochSecond.toString, event)
    }
    .via(Producer.produceAll(Serde.string, eventSerde))
    .runDrain

  override def produceEvent(event: Event): ZIO[Any, Nothing, Unit] =
    eventQueue.offer(event).unit

  override def run(): ZIO[Scope, Throwable, Unit] = kafkaStream.provideLayer(producerLayer)

}

object KafkaEventRepository {
  val live: ZLayer[AppConfiguration, String, KafkaEventRepository] = ZLayer.fromZIO {
    for {
      queue            <- Queue.unbounded[Event]
      config           <- ZIO.serviceWith[AppConfiguration](_.kafkaProducer)
      producerSettings <- ZIO.succeed(ProducerSettings(List(config.address)).withClientId(config.client))
      kafkaTopic       <- ZIO.fromEither(KafkaTopic.fromString(config.topic))
    } yield new KafkaEventRepository(queue, producerSettings, kafkaTopic)
  }
}
