package integration

import common.Generators._
import domain.repositories.ChessPieceRepository
import domain.{ChessPieceId, Position}
import doobie.implicits.toSqlInterpolator
import gateway.in.{ChessPieceIdInput, InsertPieceInput, MovePieceInput}
import gateway.out.ChessPieceIdOutput
import infrastructure.configuration.AppConfiguration
import infrastructure.migrations.DatabaseMigration
import infrastructure.repositories.{PostgresChessPieceRepository, PostgresLayers}
import io.github.gaelrenoux.tranzactio.doobie._
import io.scalaland.chimney.dsl.TransformerOps
import org.http4s.Method._
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.client.JavaNetClientBuilder
import org.http4s.{Request, Uri}
import zio.interop.catz._
import zio.test.TestAspect.{samples, sequential}
import zio.test._
import zio.{Scope, Task, ZIO}

object Integration extends ZIOSpecDefault {

  val httpClient = JavaNetClientBuilder[Task].create

  private val movePieceUri: Uri =
    Uri.unsafeFromString("http://localhost:6969/v1/board/piece/move") // todo from config
  private val insertPieceUri: Uri =
    Uri.unsafeFromString("http://localhost:6969/v1/board/piece/insert") // todo from config

  private val truncateTable = Database.transactionOrDie {
    tzio {
      sql"""TRUNCATE TABLE chess_pieces;""".update.run
    }

  }

  def movePieceRequest(position: MovePieceInput): Task[Boolean] =
    httpClient.successful(Request[Task](POST, movePieceUri).withEntity(position))

  def insertPieceRequest(inputPiece: InsertPieceInput): Task[ChessPieceIdOutput] =
    httpClient.expect[ChessPieceIdOutput](Request[Task](POST, insertPieceUri).withEntity(inputPiece))

  private val successFullyMoveAPiece = test("should successfully move a piece") {
    check(genValidDestinationFromInsertPieceInput) { case (insert, move) =>
      for {
        _              <- truncateTable
        chessPieceRepo <- ZIO.service[ChessPieceRepository]
        newPieceId     <- insertPieceRequest(insert)
        moveUpdated = move.copy(id = newPieceId.transformInto[ChessPieceIdInput])
        _                        <- movePieceRequest(moveUpdated)
        pieceWithUpdatedPosition <- chessPieceRepo.getPiece(newPieceId.transformInto[ChessPieceId])
      } yield assertTrue(pieceWithUpdatedPosition.position == move.movement.transformInto[Position])

    }
  }

  private val failsToMoveAPieceOnInvalidId = test("should successfully move a piece") {
    check(genValidDestinationFromInsertPieceInput) { case (insert, move) =>
      for {
        _ <- truncateTable
        _ <- insertPieceRequest(insert)
        e <- movePieceRequest(move).either
      } yield assertTrue(e == Right(false))

    }
  }

  private val failsToMoveAPieceIfPathIsNotClear = test("should fail is path is not clear") {
    check(genPieceAndInvalidMoveDueToInBetweenPiece) { case (insert0, insert1, move) =>
      for {
        _           <- truncateTable
        newPieceId0 <- insertPieceRequest(insert0)
        _           <- insertPieceRequest(insert1)
        moveUpdated = move.copy(id = newPieceId0.transformInto[ChessPieceIdInput])
        e <- movePieceRequest(moveUpdated).either
      } yield assertTrue(e == Right(false))

    }
  }

  private val insertingPieceShouldSucceedIfValid = test("should successfully insert a piece") {
    check(inBoardPieceInputGen) { insert =>
      for {
        _              <- truncateTable
        chessPieceRepo <- ZIO.service[ChessPieceRepository]
        newPieceId     <- insertPieceRequest(insert)
        fetchedPiece   <- chessPieceRepo.getPiece(newPieceId.transformInto[ChessPieceId])
      } yield assertTrue(newPieceId.transformInto[ChessPieceId] == fetchedPiece.id)

    }
  }

  private val insertingPieceShouldFailIfInsertedTwice = test("should fail if inserted twice") {
    check(inBoardPieceInputGen) { insert =>
      for {
        _   <- truncateTable
        _   <- insertPieceRequest(insert)
        res <- insertPieceRequest(insert).either
      } yield assertTrue(res.isLeft)

    }
  }

  val moveSuite =
    suite("moving piece")(successFullyMoveAPiece, failsToMoveAPieceOnInvalidId, failsToMoveAPieceIfPathIsNotClear)

  val insertSuite =
    suite("inserting piece")(insertingPieceShouldSucceedIfValid, insertingPieceShouldFailIfInsertedTwice)

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("integration on")(moveSuite, insertSuite).provideShared(
      DatabaseMigration.live,
      PostgresChessPieceRepository.live,
      PostgresLayers.liveConnection,
      PostgresLayers.liveDataSource,
      PostgresLayers.liveDatabase,
      AppConfiguration.live
    ) @@ sequential @@ samples(5)
}
