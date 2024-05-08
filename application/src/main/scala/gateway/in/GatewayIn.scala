package gateway.in

import java.util.UUID

import domain.ChessPieceType
import gateway.common.GatewayCodecs._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}
import sttp.tapir.EndpointIO.annotations.description

final case class ChessPieceIdInput(value: UUID)

object ChessPieceIdInput {
  implicit val chessPieceIdInputDecoder: Decoder[ChessPieceIdInput] = Decoder.decodeUUID.map(ChessPieceIdInput.apply)
  implicit val chessPieceIdInputEncoder: Encoder[ChessPieceIdInput] = Encoder.encodeUUID.contramap(_.value)
  implicit val chessPieceIdInputCodec: Codec[ChessPieceIdInput] =
    Codec.from(chessPieceIdInputDecoder, chessPieceIdInputEncoder)
}

final case class PositionInput(x: Int, y: Int)

object PositionInput {
  implicit val positionInputCodec: Codec[PositionInput] = deriveCodec
}

final case class MovePieceInput(id: ChessPieceIdInput, movement: PositionInput)

object MovePieceInput {
  implicit val movePieceCodec: Codec[MovePieceInput] = deriveCodec
}


sealed trait ChessPieceInput {
  def id: ChessPieceIdInput
  def position: PositionInput
}

final case class RookInput(id: ChessPieceIdInput, position: PositionInput)   extends ChessPieceInput
final case class BishopInput(id: ChessPieceIdInput, position: PositionInput) extends ChessPieceInput

object ChessPieceInput {
  implicit val configuration: Configuration                 = Configuration.default.withDiscriminator("type")
  implicit val chessPieceInputCodec: Codec[ChessPieceInput] = deriveConfiguredCodec[ChessPieceInput]
}

final case class InsertPieceInput(
  @description("Position of the piece to insert. Values must be between 0 and 8")
  position: PositionInput,
  @description("Type of piece to insert. Valid values are [Rook, Bishop]")
  kind: ChessPieceType
)

object InsertPieceInput {
  implicit val insertPieceInputCodec: Codec[InsertPieceInput] = deriveCodec
}
