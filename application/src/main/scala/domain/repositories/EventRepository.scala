package domain.repositories

import zio.{Scope, ZIO}

final case class Event(event: String)

trait EventRepository {

  def produceEvent(event: Event): ZIO[Any, Nothing, Unit]
  def run(): ZIO[Scope, Throwable, Unit]

}
