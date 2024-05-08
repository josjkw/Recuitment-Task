package domain.services

import domain._
import domain.repositories.{ChessPieceRepository, Event}
import infrastructure.configuration.AppConfiguration
import infrastructure.repositories.{KafkaEventRepository, PostgresChessPieceRepository}
import io.github.gaelrenoux.tranzactio.DbException
import io.github.gaelrenoux.tranzactio.doobie.{Connection, Database}
import zio.{Scope, ZIO, ZLayer}

class BoardService(chessPieceRepository: ChessPieceRepository)(eventService: EventService) {

  def movePiece(chessPieceId: ChessPieceId, newPosition: Position): ZIO[Connection with Database, String, Unit] = for {
    currentChessPiece <- Database.transactionOrDie {
      for {
        allPieces         <- chessPieceRepository.getAllPieces.mapBoth(_.toString, _.filterNot(_.id == chessPieceId))
        currentChessPiece <- chessPieceRepository.getPiece(chessPieceId).orElseFail("Piece to move not found")
        _                 <- currentChessPiece.isLegalMove(newPosition, allPieces)
        _                 <- chessPieceRepository.updatePiecePosition(chessPieceId, newPosition).mapError(_.toString)
      } yield currentChessPiece
    }
    _ <- eventService.produceEvent(Event(s"Piece ${currentChessPiece.id} moved"))
  } yield ()

  def deletePiece(chessPieceId: ChessPieceId): ZIO[Connection, DbException, Unit] = for {
    _ <- chessPieceRepository.deletePiece(chessPieceId)
    _ <- eventService.produceEvent(Event(s"Piece $chessPieceId deleted"))
  } yield ()

  def insertPiece(chessPiece: ChessPiece): ZIO[Connection with Database, String, Unit] = for {

    _ <- Database.transactionOrDie {
      for {
        isSquareAvailable <- chessPieceRepository.isPositionFree(chessPiece.position).mapError(_.toString)
        _                 <- ZIO.fail("This square isn't available for your piece").unless(isSquareAvailable).unit
        _                 <- chessPieceRepository.insertPiece(chessPiece).mapError(_.toString)
      } yield ()
    }
    _ <- eventService.produceEvent(Event(s"Piece ${chessPiece.id} added"))
  } yield ()

}

object BoardService {
  def live = ZLayer {
    for {
      chessPieceRepository <- ZIO.service[ChessPieceRepository]
      eventService         <- ZIO.service[EventService]
    } yield new BoardService(chessPieceRepository)(eventService)
  }

  def make = ZLayer.make[BoardService](
    BoardService.live,
    PostgresChessPieceRepository.live,
    EventService.live,
    KafkaEventRepository.live,
    Scope.default,
    AppConfiguration.live
  )
}
