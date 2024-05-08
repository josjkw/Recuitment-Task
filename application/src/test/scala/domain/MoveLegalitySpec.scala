package domain

import common.Generators._
import zio.Scope
import zio.test.Assertion.{equalTo, fails}
import zio.test._

object MoveLegalitySpec extends ZIOSpecDefault {

  private val shouldReturnTrueIfRookMovementIsValid = test("should succeed is rook move is valid") {
    val genPieceAndValidPosition = for {
      chessPiece       <- rookGen
      validDestination <- validRookDestinationGen(chessPiece.position)
    } yield (chessPiece, validDestination)
    check(genPieceAndValidPosition) { case (piece, validDestination) =>
      piece.isLegalMove(validDestination, List.empty).as(assertCompletes)
    }
  }

  private val shouldFailIfRookMoveIsInvalid = test("should fail if rook move is invalid") {
    val genPieceAndInvalidPosition = for {
      chessPiece         <- rookGen
      invalidDestination <- invalidRookDestinationGen(chessPiece.position)
    } yield (chessPiece, invalidDestination)
    check(genPieceAndInvalidPosition) { case (rook, invalidDestination) =>
      val res = (for {
        _ <- rook.isLegalMove(invalidDestination, List.empty)
      } yield ()).exit
      assertZIO(res)(fails(equalTo("Illegal rook move")))
    }
  }

  private val shouldFailIfAPieceIfAPieceIsInBetweenRook = test("should fail if rook move has a piece blocking it") {
    check(pieceInBetweenRookMoveGen) { case (rook, validDestination, pieces) =>
      val res = (for {
        res <- rook.isLegalMove(validDestination, pieces)
      } yield res).exit
      assertZIO(res)(fails(equalTo("You can't move, a piece is in between!")))
    }
  }

  private val rookSuite = suite("Rook")(
    shouldReturnTrueIfRookMovementIsValid,
    shouldFailIfRookMoveIsInvalid,
    shouldFailIfAPieceIfAPieceIsInBetweenRook
  )

  private val shouldReturnUnitIfBishopMovementIsValid = test("should return unit if bishop movement is valid") {
    val bishopAndValidDestination = for {
      bishop           <- bishopGen
      validDestination <- validBishopDestinationGen(bishop.position)
    } yield bishop -> validDestination
    check(bishopAndValidDestination) { case (bishop, validDestination) =>
      bishop.isLegalMove(validDestination, List.empty).as(assertCompletes)
    }

  }

  private val shouldFailIfBishopMovementIsInvalid = test("should fail if bishop movement is invalid") {
    val bishopAndInvalidDestination = for {
      bishop             <- bishopGen
      invalidDestination <- invalidBishopDestinationGen(bishop.position)
    } yield bishop -> invalidDestination

    check(bishopAndInvalidDestination) { case (bishop, invalidDestination) =>
      val res = (for {
        res <- bishop.isLegalMove(invalidDestination, List.empty)
      } yield res).exit
      assertZIO(res)(fails(equalTo("Illegal bishop move")))
    }

  }

  private val shouldFailIfPieceIsBlockingBishop = test(" should fail if piece is blocking bishop movement") {
    check(pieceInBetweenBishopMoveGen) { case (bishop, validDestination, pieces) =>
      val res = (for {
        res <- bishop.isLegalMove(validDestination, pieces)
      } yield res).exit
      assertZIO(res)(fails(equalTo("You can't move, a piece is in between!")))
    }
  }

  private val bishopSuite = suite("Bishop")(
    shouldReturnUnitIfBishopMovementIsValid,
    shouldFailIfBishopMovementIsInvalid,
    shouldFailIfPieceIsBlockingBishop
  )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("BoardValidatorSpec")(rookSuite, bishopSuite)
}
