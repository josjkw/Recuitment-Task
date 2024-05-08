package gateway

import java.util.UUID

import domain.services.BoardService
import domain.{ChessPiece, ChessPieceId, ChessPieceType, Position}
import gateway.in.{BishopInput, ChessPieceIdInput, ChessPieceInput, InsertPieceInput, MovePieceInput, RookInput}
import gateway.out.ChessPieceIdOutput
import gateway.transformers.ApiToDomainTransformers._
import io.github.gaelrenoux.tranzactio.doobie.{Connection, Database}
import io.scalaland.chimney.dsl.TransformerOps
import sttp.model.StatusCode
import sttp.tapir.Codec.JsonCodec
import sttp.tapir.json.circe._
import sttp.tapir.ztapir._
import zio.{RIO, ZIO}

object BoardRoutes {
  import gateway.common.GatewaySchemas._

  type Env = Database with Connection with BoardService

  type AppTask[A] = RIO[Env, A]

  private val apiVersion = "v1"

  private val base            = apiVersion / "board" / "piece"
  private val movePiecePath   = base / "move"
  private val deletePiecePath = base / "delete"
  private val insertPiecePath = base / "insert"

  private val movePieceTapirCodec: JsonCodec[MovePieceInput]        = circeCodec
  private val chessPieceIdInputCodec: JsonCodec[ChessPieceIdInput]  = circeCodec
  private val chessPieceIdOut: JsonCodec[ChessPieceIdOutput]        = circeCodec
  private val chessPieceInputTapirCodec: JsonCodec[ChessPieceInput] = circeCodec
  private val insertPieceInputCodec: JsonCodec[InsertPieceInput]    = circeCodec

  private val movePieceEndpoint: ZServerEndpoint[Env, Any] =
    endpoint.post
      .name("movePiece")
      .summary("Moves a piece")
      .in(movePiecePath.and(jsonBody[MovePieceInput]))
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode(StatusCode.BadRequest).and(plainBody[String]))
      .zServerLogic { movePiece =>
        for {
          _ <- movePieceHandler(movePiece)
        } yield ()
      }

  private val deletePieceEndpoint: ZServerEndpoint[Env, Any] =
    endpoint.post
      .name("deletePiece")
      .summary("Deletes a piece")
      .in(deletePiecePath.and(jsonBody[ChessPieceIdInput]))
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode(StatusCode.BadRequest).and(plainBody[String]))
      .zServerLogic { chessPieceIdApi =>
        for {
          _ <- deletePieceHandler(chessPieceIdApi).mapError(_.toString)
        } yield ()

      }

  private val insertPieceEndpoint: ZServerEndpoint[Env, Any] =
    endpoint.post
      .name("insertPiece")
      .summary("Inserts a piece")
      .in(insertPiecePath.and(jsonBody[InsertPieceInput]))
      .out(statusCode(StatusCode.Ok).and(jsonBody[ChessPieceIdOutput]))
      .errorOut(statusCode(StatusCode.BadRequest).and(plainBody[String]))
      .zServerLogic { insertPiece =>
        val pieceIdApi = ChessPieceIdInput(UUID.randomUUID())

        for {
          chessPieceApi <- insertPiece.kind match {
            case ChessPieceType.Rook   => ZIO.succeed(RookInput(pieceIdApi, insertPiece.position))
            case ChessPieceType.Bishop => ZIO.succeed(BishopInput(pieceIdApi, insertPiece.position))
          }
          _ <- insertPieceHandler(chessPieceApi)
        } yield pieceIdApi.transformInto[ChessPieceIdOutput]
      }

  private def movePieceHandler(movePiece: MovePieceInput) =
    ZIO.serviceWithZIO[BoardService](
      _.movePiece(movePiece.id.transformInto[ChessPieceId], movePiece.movement.transformInto[Position])
    )

  private def deletePieceHandler(pieceId: ChessPieceIdInput) =
    ZIO.serviceWithZIO[BoardService](_.deletePiece(pieceId.transformInto[ChessPieceId]))

  private def insertPieceHandler(pieceToInsert: ChessPieceInput) =
    ZIO.serviceWithZIO[BoardService](_.insertPiece(pieceToInsert.transformInto[ChessPiece]))

  val boardEndpoints: List[ZServerEndpoint[Env, Any]] =
    List(movePieceEndpoint, deletePieceEndpoint, insertPieceEndpoint)

}
