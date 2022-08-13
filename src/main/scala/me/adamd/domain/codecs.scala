package me.adamd.domain

import io.circe.{Encoder, Json, Decoder, DecodingFailure}
import io.circe.syntax._
import cats.syntax.either._
import io.circe.generic.semiauto._
import me.adamd.domain.models._
import me.adamd.domain.models.types._

trait JsonCodecs:
  given idEncoder: Encoder[SchemaId] = x => Json.fromString(x.value)

  given schemaEncoder: Encoder[Schema] = _.value
  given documentEncoder: Encoder[Document] = _.value

  given schemaDecoder: Decoder[Schema] = x =>
    Schema(x.value.deepDropNullValues).asRight[DecodingFailure]

  given documentDecoder: Decoder[Document] = x =>
    Document(x.value.deepDropNullValues).asRight[DecodingFailure]

  given actionEncoder: Encoder[Action] = Encoder.instance {
    case Action.GetSchema        => "getSchema".asJson
    case Action.UploadSchema     => "uploadSchema".asJson
    case Action.ValidateDocument => "validateDocument".asJson
  }

  given successEncoder: Encoder[Success] = Encoder.instance { case x =>
    Json.obj(
      "id"     -> x.schemaId.value.asJson,
      "action" -> x.action.asJson,
      "status" -> "success".asJson
    )
  }

  given failureEncoder: Encoder[Failure] = Encoder.instance { case x =>
    Json.obj(
      "id"      -> x.schemaId.value.asJson,
      "action"  -> x.action.asJson,
      "message" -> x.message.asJson,
      "status"  -> "error".asJson
    )
  }

  given responseEncoder: Encoder[SchemaValidation] = Encoder.instance {
    case x: Success => x.asJson; case x: Failure => x.asJson
  }
