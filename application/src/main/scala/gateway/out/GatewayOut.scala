package gateway.out

import java.util.UUID

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class ChessPieceIdOutput(value: UUID)

object ChessPieceIdOutput {
  implicit val chessPieceIdOutputCodec: Codec[ChessPieceIdOutput] = deriveCodec
}
