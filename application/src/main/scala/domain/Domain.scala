package domain

import java.util.UUID

import enumeratum.{Enum, EnumEntry}
import zio.ZIO

final case class Position(x: Int, y: Int)
final case class ChessPieceId(value: UUID)

sealed trait ChessPiece {
  def id: ChessPieceId
  def position: Position

  protected def checkHorizontalPath(to: Position, pieces: List[ChessPiece]): ZIO[Any, String, Unit] = {
    val minX = math.min(position.x, to.x)
    val maxX = math.max(position.x, to.x)
    val isAPieceInTheMiddle = !pieces.exists { piece =>
      piece.position.x >= minX && piece.position.x <= maxX && piece.position.y == position.y
    }
    ZIO.fail("You can't move, a piece is in between!").unless(isAPieceInTheMiddle).unit
  }
  protected def checkVerticalPath(to: Position, pieces: List[ChessPiece]): ZIO[Any, String, Unit] = {
    val minY = math.min(position.y, to.y)
    val maxY = math.max(position.y, to.y)
    val isAPieceInTheMiddle = !pieces.exists { piece =>
      piece.position.y >= minY && piece.position.y <= maxY && piece.position.x == position.x
    }
    ZIO.fail("You can't move, a piece is in between!").unless(isAPieceInTheMiddle).unit
  }

  protected def checkDiagonalPath(to: Position, pieces: List[ChessPiece]): ZIO[Any, String, Unit] = {
    val dx = if (position.x < to.x) 1 else -1
    val dy = if (position.y < to.y) 1 else -1

    val path =
      (position.x + dx to to.x by dx).zip(position.y + dy to to.y by dy).foldLeft(List.empty[Position]) {
        case (acc, (x, y)) =>
          acc appended Position(x, y)
      }
    val isThePathClear = pieces.map(_.position).intersect(path).isEmpty
    ZIO.fail("You can't move, a piece is in between!").unless(isThePathClear).unit
  }

  def isLegalMove(to: Position, pieces: List[ChessPiece]): ZIO[Any, String, Unit] =
    this.isLegalOnEmptyChessboard(to) *> this.isPathClearImplementation(to, pieces)

  protected def isLegalOnEmptyChessboard(to: Position): ZIO[Any, String, Unit]
  protected def isPathClearImplementation(to: Position, pieces: List[ChessPiece]): ZIO[Any, String, Unit]
}

final case class Rook(id: ChessPieceId, position: Position) extends ChessPiece {

  override protected def isLegalOnEmptyChessboard(to: Position): ZIO[Any, String, Unit] = {
    val onlyMovesInOneDimension = (position.x == to.x) || (position.y == to.y)
    val movesAtAll              = (position.x != to.x) || (position.y != to.y)
    ZIO.fail("Illegal rook move").unless(onlyMovesInOneDimension && movesAtAll).unit
  }

  override protected def isPathClearImplementation(to: Position, pieces: List[ChessPiece]): ZIO[Any, String, Unit] = {
    val isMovingHorizontally = position.y == to.y
    if (isMovingHorizontally) {
      checkHorizontalPath(to, pieces)
    } else {
      checkVerticalPath(to, pieces)
    }
  }
}

final case class Bishop(id: ChessPieceId, position: Position) extends ChessPiece {

  override protected def isLegalOnEmptyChessboard(to: Position): ZIO[Any, String, Unit] = {
    val isADiagonalMove = Math.abs(position.x - to.x) == Math.abs(position.y - to.y)
    val movesAtAll      = (position.x != to.x) && (position.y != to.y)
    ZIO.fail("Illegal bishop move").unless(isADiagonalMove && movesAtAll).unit
  }

  override protected def isPathClearImplementation(to: Position, pieces: List[ChessPiece]): ZIO[Any, String, Unit] =
    checkDiagonalPath(to: Position, pieces: List[ChessPiece])
}

sealed trait ChessPieceType extends EnumEntry
object ChessPieceType extends Enum[ChessPieceType] {

  override def values: IndexedSeq[ChessPieceType] = findValues

  case object Rook   extends ChessPieceType
  case object Bishop extends ChessPieceType

  def fromString(s: String): Either[String, ChessPieceType] =
    ChessPieceType.withNameOption(s).toRight(s"Unknown enum value: $s")
}
