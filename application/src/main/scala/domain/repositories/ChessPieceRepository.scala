package domain.repositories

import domain.{ChessPiece, ChessPieceId, Position}
import io.github.gaelrenoux.tranzactio.DbException
import io.github.gaelrenoux.tranzactio.doobie.Connection
import zio.ZIO

trait ChessPieceRepository {

  def insertPiece(chessPiece: ChessPiece): ZIO[Connection, DbException, Unit]
  def deletePiece(chessPieceId: ChessPieceId): ZIO[Connection, DbException, Unit]
  def updatePiecePosition(chessPieceId: ChessPieceId, position: Position): ZIO[Connection, DbException, Unit]
  def getAllPieces: ZIO[Connection, DbException, List[ChessPiece]]
  def getPiece(chessPieceId: ChessPieceId): ZIO[Connection, DbException, ChessPiece]
  def isPositionFree(position: Position): ZIO[Connection, DbException, Boolean]

}
