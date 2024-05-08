package gateway.common

import domain.ChessPieceType
import enumeratum.Circe
import io.circe.Codec

object GatewayCodecs {

  implicit val chessPieceTypeInputCodec: Codec[ChessPieceType] =
    Codec.from(
      Circe.decoder(ChessPieceType),
      Circe.encoder(ChessPieceType)
    )

}
