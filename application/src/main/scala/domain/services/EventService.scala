package domain.services

import domain.repositories.{Event, EventRepository}
import zio.{Scope, ZIO, ZLayer}

class EventService(eventRepository: EventRepository) {

  def produceEvent(event: Event): ZIO[Any, Nothing, Unit] = eventRepository.produceEvent(event)

}

object EventService {
  val live: ZLayer[EventRepository with Scope, Throwable, EventService] =
    ZLayer.fromZIO {
      for {
        eventRepository <- ZIO.service[EventRepository]
        _               <- eventRepository.run().fork
      } yield new EventService(eventRepository)
    }
}
