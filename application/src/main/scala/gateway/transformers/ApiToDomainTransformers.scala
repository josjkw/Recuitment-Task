package gateway.transformers

import domain.{Bishop, ChessPiece, Rook}
import gateway.in.{BishopInput, ChessPieceInput, RookInput}
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl.TransformerOps

object ApiToDomainTransformers {

  implicit val chessPieceApiToChessPiece: Transformer[ChessPieceInput, ChessPiece] = {
    case rook @ RookInput(_, _)     => rook.transformInto[Rook]
    case bishop @ BishopInput(_, _) => bishop.transformInto[Bishop]
  }

}
