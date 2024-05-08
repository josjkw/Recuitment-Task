package infrastructure.repositories

import java.util.UUID

import domain.repositories.ChessPieceRepository
import domain.{Bishop, ChessPiece, ChessPieceId, ChessPieceType, Position, Rook}
import doobie.implicits.toSqlInterpolator
import doobie.postgres.implicits._
import doobie.util.{Get, transactor}
import io.github.gaelrenoux.tranzactio.DbException
import io.github.gaelrenoux.tranzactio.doobie._
import zio.{Task, ZIO, ZLayer}

class PostgresChessPieceRepository extends ChessPieceRepository {

  private type ChessPieceColumns = (UUID, Int, Int, Boolean, ChessPieceType)
  implicit val getChessPieceType: Get[ChessPieceType] = Get[String].temap(ChessPieceType.fromString)

  override def updatePiecePosition(
    chessPieceId: ChessPieceId,
    position: Position
  ): ZIO[Connection, DbException, Unit] =
    tzio {
      val idFragment = fr"${chessPieceId.value}"
      sql"""
        UPDATE chess_pieces
        SET position_x = ${position.x}, position_y = ${position.y}
        WHERE id = $idFragment
      """.update.run

    }.unit

  override def insertPiece(chessPiece: ChessPiece): ZIO[transactor.Transactor[Task], DbException, Unit] =
    tzio {
      val idFragment = fr"${chessPiece.id.value}"
      val pieceType  = fr"${chessPiece.getClass.getSimpleName}"

      sql"""
        INSERT INTO chess_pieces (id, position_x, position_y, is_on_board, type)
        VALUES ($idFragment, ${chessPiece.position.x}, ${chessPiece.position.y}, true, $pieceType)
      """.update.run

    }.unit

  override def deletePiece(chessPieceId: ChessPieceId): ZIO[Connection, DbException, Unit] =
    tzio {
      val idFragment = fr"${chessPieceId.value}"
      sql"""
        UPDATE chess_pieces
        SET is_on_board = false
        WHERE id = $idFragment
      """.update.run

    }.unit

  override def getAllPieces: ZIO[Connection, DbException, List[ChessPiece]] = tzio {
    sql"""
           SELECT * FROM chess_pieces WHERE is_on_board = true
         """
      .query[ChessPieceColumns]
      .to[List]
      .map(_.map(toChessPiece))
  }

  override def getPiece(chessPieceId: ChessPieceId): ZIO[Connection, DbException, ChessPiece] = tzio {
    sql"""
           SELECT * FROM chess_pieces
           WHERE is_on_board = true
           AND id = $chessPieceId
         """
      .query[ChessPieceColumns]
      .unique
      .map(toChessPiece)
  }

  override def isPositionFree(position: Position): ZIO[Connection, DbException, Boolean] = tzio {
    sql"""
           SELECT * FROM chess_pieces
           WHERE is_on_board = true
           AND position_x = ${position.x}
           AND position_y = ${position.y}
         """
      .query[ChessPieceColumns]
      .option
      .map(_.isEmpty)
  }

  private def toChessPiece(data: ChessPieceColumns) = {
    val (uuid, x, y, _, pieceType) = data
    pieceType match {
      case ChessPieceType.Rook   => Rook(ChessPieceId(uuid), Position(x, y))
      case ChessPieceType.Bishop => Bishop(ChessPieceId(uuid), Position(x, y))
    }
  }

}

object PostgresChessPieceRepository {
  def live = ZLayer.succeed(
    new PostgresChessPieceRepository()
  )
}
