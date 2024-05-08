package infrastructure

import common.Generators._
import common.PostgresTestContainer
import common.PostgresTestContainer.PostgresContainerConfig
import infrastructure.configuration.{AppConfiguration, HttpServer, KafkaProducer, Postgres}
import infrastructure.migrations.DatabaseMigration
import infrastructure.repositories.{PostgresChessPieceRepository, PostgresLayers}
import zio.test.Assertion.{anything, fails}
import zio.test._
import zio.{Scope, ZIO, ZLayer}

import scala.util.Random

object PostgresChessPieceRepositorySpec extends ZIOSpecDefault {

  val appConfig: ZLayer[PostgresContainerConfig, Nothing, AppConfiguration] = ZLayer.fromZIO {
    ZIO.serviceWith[PostgresContainerConfig] { postgresContainerConfig =>
      AppConfiguration(
        kafkaProducer = KafkaProducer(
          address = "foo",
          client = "foo",
          topic = "foo"
        ),
        postgres = Postgres(
          address = postgresContainerConfig.address,
          user = postgresContainerConfig.user,
          password = postgresContainerConfig.password
        ),
        httpServer = HttpServer(
          port = 1234,
          host = "foo"
        )
      )
    }
  }

  private val validInsertShouldSucceed = test("should succeed") {
    check(chessPieceGen) { piece =>
      for {
        postgresChessPieceRepository <- ZIO.service[PostgresChessPieceRepository]
        res                          <- postgresChessPieceRepository.insertPiece(piece)
      } yield assertTrue(res == ())
    }
  }

  private val shouldFailIfAlreadyExist = test("should fail if piece already exists") {
    check(chessPieceGen) { piece =>
      val res = (for {
        postgresChessPieceRepository <- ZIO.service[PostgresChessPieceRepository]
        _                            <- postgresChessPieceRepository.insertPiece(piece)
        res                          <- postgresChessPieceRepository.insertPiece(piece)
      } yield res).exit
      assertZIO(res)(fails(anything))
    }
  }

  private val validUpdateShouldSucceed = test("should succeed") {
    check(chessPieceGen, positionGen) { (piece, position) =>
      for {
        postgresChessPieceRepository <- ZIO.service[PostgresChessPieceRepository]
        _                            <- postgresChessPieceRepository.insertPiece(piece)
        _                            <- postgresChessPieceRepository.updatePiecePosition(piece.id, position)
        newPos <- postgresChessPieceRepository.getAllPieces.map(_.find(_.id == piece.id)).map(_.get.position)
      } yield assertTrue(newPos == position)
    }
  }

  private val validDeleteShouldSucceed = test("should succeed") {
    check(chessPieceGen) { piece =>
      for {
        postgresChessPieceRepository <- ZIO.service[PostgresChessPieceRepository]
        _                            <- postgresChessPieceRepository.insertPiece(piece)
        _                            <- postgresChessPieceRepository.deletePiece(piece.id)
        pieceOpt                     <- postgresChessPieceRepository.getAllPieces.map(_.find(_.id == piece.id))
      } yield assertTrue(pieceOpt.isEmpty)
    }
  }

  private val getAllPiecesShouldSucceed = test("should succeed") {
    check(chessPieceGen, chessPieceGen) { (piece0, piece1) =>
      for {
        postgresChessPieceRepository <- ZIO.service[PostgresChessPieceRepository]
        _                            <- postgresChessPieceRepository.insertPiece(piece0)
        _                            <- postgresChessPieceRepository.insertPiece(piece1)
        res <- postgresChessPieceRepository.getAllPieces.map(pieces =>
          pieces.contains(piece0) && pieces.contains(piece1)
        )
      } yield assertTrue(res)
    }
  }

  private val getPieceShouldSucceed = test("should succeed") {
    check(chessPieceGen) { piece =>
      for {
        postgresChessPieceRepository <- ZIO.service[PostgresChessPieceRepository]
        _                            <- postgresChessPieceRepository.insertPiece(piece)
        queriedPiece                 <- postgresChessPieceRepository.getPiece(piece.id)
      } yield assertTrue(queriedPiece == piece)
    }
  }

  private val getPieceShouldFailIfPieceDoesNotExist = test("should fail if piece doesn't exist") {
    check(chessPieceGen) { piece =>
      val res = (for {
        postgresChessPieceRepository <- ZIO.service[PostgresChessPieceRepository]
        _                            <- postgresChessPieceRepository.getPiece(piece.id)
      } yield ()).exit
      assertZIO(res)(fails(anything))
    }
  }

  private val isPositionFreeShouldReturnTrueOnEmptySquare = test("should return true on empty square") {
    // test could be more precise
    check(positionGen) { position =>
      for {
        postgresChessPieceRepository <- ZIO.service[PostgresChessPieceRepository]
        isPositionFree               <- postgresChessPieceRepository.isPositionFree(position)
      } yield assertTrue(isPositionFree)
    }
  }

  private val isPositionFreeShouldReturnFalseOnOccupiedSquare = test("should return true on empty square") {
    // test could be more precise
    check(chessPieceGen) { piece =>
      for {
        postgresChessPieceRepository <- ZIO.service[PostgresChessPieceRepository]
        _                            <- postgresChessPieceRepository.insertPiece(piece)
        isPositionFree               <- postgresChessPieceRepository.isPositionFree(piece.position)
      } yield assertTrue(!isPositionFree)
    }
  }

  private val insertSuite       = suite("on insert")(validInsertShouldSucceed, shouldFailIfAlreadyExist)
  private val updateSuite       = suite("on update")(validUpdateShouldSucceed)
  private val deleteSuite       = suite("on delete")(validDeleteShouldSucceed)
  private val getAllPiecesSuite = suite("on get all pieces")(getAllPiecesShouldSucceed)
  private val getPieceSuite     = suite("on get piece")(getPieceShouldSucceed, getPieceShouldFailIfPieceDoesNotExist)
  private val isPositionFreeSuite = suite("is position free")(
    isPositionFreeShouldReturnTrueOnEmptySquare,
    isPositionFreeShouldReturnFalseOnOccupiedSquare
  )

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("PostgresChessPieceRepository")(
      insertSuite,
      updateSuite,
      deleteSuite,
      getAllPiecesSuite,
      getPieceSuite,
      isPositionFreeSuite
    )
      .provideShared(
        PostgresLayers.liveConnection,
        PostgresLayers.liveDataSource,
        PostgresChessPieceRepository.live,
        DatabaseMigration.live,
        PostgresTestContainer.live,
        appConfig,
        Scope.default
      ) @@ TestAspect.setSeed(Random.nextLong())
  }
}
