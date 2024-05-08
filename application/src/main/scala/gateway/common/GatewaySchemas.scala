package gateway.common

import domain.ChessPieceType
import gateway.in.{ChessPieceIdInput, ChessPieceInput, InsertPieceInput, MovePieceInput, PositionInput}
import gateway.out.ChessPieceIdOutput
import sttp.tapir.codec.enumeratum.schemaForEnumEntry
import sttp.tapir.{Schema, ValidationResult, Validator}

object GatewaySchemas {

  private def positionValidator: Validator[PositionInput] = Validator.custom[PositionInput] {
    case PositionInput(x, y) if x < 0 || x > 8 || y < 0 || y > 8 =>
      ValidationResult.Invalid("Position should be between 0 and 8")
    case _ => ValidationResult.Valid
  }

  implicit val chessPieceIdApiSchema: Schema[ChessPieceIdInput] =
    Schema.schemaForUUID.map(uuid => Some(ChessPieceIdInput(uuid)))(cpi => cpi.value)

  implicit val chessPieceIdOutput: Schema[ChessPieceIdOutput] = Schema.derived

  implicit val positionApiSchema: Schema[PositionInput] =
    Schema.derived[PositionInput].validate(positionValidator)

  implicit val movePieceSchema: Schema[MovePieceInput] = Schema.derived

  implicit val chessPieceApiSchema: Schema[ChessPieceInput] = Schema.derived

  implicit val chessPieceTypeInputSchema: Schema[ChessPieceType] = schemaForEnumEntry

  implicit val insertPieceApiSchema: Schema[InsertPieceInput] = Schema.derived

}
