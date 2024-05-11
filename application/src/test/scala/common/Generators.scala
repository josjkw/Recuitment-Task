package common

import domain._
import gateway.in.{InsertPieceInput, MovePieceInput, PositionInput}
import io.scalaland.chimney.dsl.TransformerOps
import zio.test.Gen
import zio.test.magnolia.DeriveGen

object Generators {

  implicit val chessPieceIdDeriveGen: DeriveGen[ChessPieceId] = DeriveGen.gen[ChessPieceId]
  val chessPieceIdGen: Gen[Any, ChessPieceId]                 = chessPieceIdDeriveGen.derive

  implicit val positionDeriveGen: DeriveGen[Position] = DeriveGen.gen[Position]
  val positionGen: Gen[Any, Position]                 = positionDeriveGen.derive
  val genInboardPosition: Gen[Any, Position] = for {
    (x, y)          <- Gen.int(0, 8).zip(Gen.int(0, 8))
    inBoardPosition <- positionGen.map(_.copy(x = x, y = y))
  } yield inBoardPosition

  val positionInputInBoardGen: Gen[Any, PositionInput] = genInboardPosition.map(_.transformInto[PositionInput])

  implicit val chessPieceDeriveGen: DeriveGen[ChessPiece] = DeriveGen.gen[ChessPiece]
  val chessPieceGen: Gen[Any, ChessPiece]                 = chessPieceDeriveGen.derive

  implicit val rookDeriveGen: DeriveGen[Rook] = DeriveGen.gen[Rook]
  val rookGen: Gen[Any, Rook]                 = rookDeriveGen.derive

  implicit val bishopDeriveGen: DeriveGen[Bishop] = DeriveGen.gen[Bishop]
  val bishopGen: Gen[Any, Bishop]                 = bishopDeriveGen.derive

  private def replacePosition(chessPiece: ChessPiece, position: Position): ChessPiece = {
    chessPiece match {
      case Rook(id, _)   => Rook(id, position)
      case Bishop(id, _) => Bishop(id, position)
    }
  }

  val pieceInBetweenRookMoveGen = for {
    rook             <- rookGen
    validDestination <- validRookDestinationGen(rook.position)
    minX = math.min(rook.position.x, validDestination.x)
    maxX = math.max(rook.position.x, validDestination.x)
    x <- Gen.int(minX, maxX)
    minY = math.min(rook.position.y, validDestination.y)
    maxY = math.max(rook.position.y, validDestination.y)
    y      <- Gen.int(minY, maxY)
    pieces <- Gen.listOf1(chessPieceGen).map(_.map(replacePosition(_, Position(x, y))))
  } yield (rook, validDestination, pieces)

  def invalidRookDestinationGen(from: Position): Gen[Any, Position] = Gen.const(Position(from.x + 1, from.y + 1))
  def validRookDestinationGen(from: Position): Gen[Any, Position] = for {
    distance <- Gen.int(-8, 8)
    xMovee = positionGen.filterNot(p => p.x == from.x).map(_.copy(x = from.x, y = distance))
    yMovee = positionGen.filterNot(p => p.y == from.y).map(_.copy(x = distance, y = from.y))
    xMove  = Gen.const(Position(from.x, distance)).filterNot(p => p.x == from.x)
    yMove  = Gen.const(Position(distance, from.y)).filterNot(p => p.y == from.y)
    validDestination <- Gen.oneOf(xMovee, yMove)
  } yield validDestination

  // slow generator but for convenience purposes
  def validRookOnBoardDestinationGen(from: Position): Gen[Any, Position] =
    validRookDestinationGen(from).filterNot(p => p.x < 0 || p.x > 8 || p.y < 0 || p.y > 8)

  def invalidBishopDestinationGen(from: Position): Gen[Any, Position] = Gen.const(Position(from.x + 1, from.y))
  def validBishopDestinationGen(from: Position): Gen[Any, Position] = for {
    distance <- Gen.int(-8, 8).filterNot(_ == 0)
    diagMove = Position(from.x + distance, from.y + distance)
  } yield diagMove

  def validBishopOnBoardDestinationGen(from: Position): Gen[Any, Position] =
    validBishopDestinationGen(from).filterNot(p => p.x < 0 || p.x > 8 || p.y < 0 || p.y > 8)

  val pieceInBetweenBishopMoveGen: Gen[Any, (Bishop, Position, List[ChessPiece])] = for {
    bishop           <- bishopGen
    validDestination <- validBishopDestinationGen(bishop.position)
    dx = if (bishop.position.x < validDestination.x) 1 else -1
    dy = if (bishop.position.y < validDestination.y) 1 else -1

    visitedSquares = (bishop.position.x + dx to validDestination.x by dx)
      .zip(bishop.position.y + dy to validDestination.y by dy)
      .foldLeft(List.empty[Position]) { case (acc, (x, y)) =>
        acc appended Position(x, y)
      }
    oneOfVisitedSquares <- Gen.fromIterable(visitedSquares)
    pieces              <- Gen.listOf1(chessPieceGen).map(_.map(replacePosition(_, oneOfVisitedSquares)))
  } yield (bishop, validDestination, pieces)

  implicit val insertPieceInputDeriveGen: DeriveGen[InsertPieceInput] = DeriveGen.gen[InsertPieceInput]
  val insertPieceInputGen: Gen[Any, InsertPieceInput]                 = insertPieceInputDeriveGen.derive
  val inBoardPieceInputGen: Gen[Any, InsertPieceInput] = for {
    inBoardPosition   <- genInboardPosition
    inBoardInputPiece <- insertPieceInputGen.map(_.copy(position = inBoardPosition.transformInto[PositionInput]))
  } yield inBoardInputPiece

  implicit val movePieceInputDeriveGen: DeriveGen[MovePieceInput] = DeriveGen.gen[MovePieceInput]
  implicit val movePieceInputGen: Gen[Any, MovePieceInput]        = movePieceInputDeriveGen.derive

  def genValidDestinationFromInsertPieceInput: Gen[Any, (InsertPieceInput, MovePieceInput)] = for {
    insertPiece <- inBoardPieceInputGen
    validMovement <- insertPiece.kind match {
      case ChessPieceType.Rook   => validRookOnBoardDestinationGen(insertPiece.position.transformInto[Position])
      case ChessPieceType.Bishop => validBishopOnBoardDestinationGen(insertPiece.position.transformInto[Position])
    }
    movePiece <- movePieceInputGen.map(_.copy(movement = validMovement.transformInto[PositionInput]))
  } yield insertPiece -> movePiece

  def genPieceAndInvalidMoveDueToInBetweenPiece: Gen[Any, (InsertPieceInput, InsertPieceInput, MovePieceInput)] = for {
    insertPiece0 <- inBoardPieceInputGen
    validMovement <- insertPiece0.kind match {
      case ChessPieceType.Rook   => validRookOnBoardDestinationGen(insertPiece0.position.transformInto[Position])
      case ChessPieceType.Bishop => validBishopOnBoardDestinationGen(insertPiece0.position.transformInto[Position])
    }
    movePiece <- movePieceInputGen.map(_.copy(movement = validMovement.transformInto[PositionInput]))
    insertPiece1 = insertPiece0.copy(position = validMovement.transformInto[PositionInput])
  } yield (insertPiece0, insertPiece1, movePiece)

}
