package me.adamd.domain

import cats.syntax.option._
import io.circe.Json

object models:
  import types._

  object types:
    opaque type SchemaId  = String
    opaque type RequestId = String

    object SchemaId:
      def apply: String => SchemaId             = identity
      extension (x: SchemaId) def value: String = x

    object RequestId:
      def apply: String => RequestId             = identity
      extension (x: RequestId) def value: String = x

  case class Schema(value: Json)

  case class Document(value: Json)

  enum Action:
    case ValidateDocument
    case UploadSchema
    case GetSchema

  enum Cause:
    case NonExist
    case InvalidSchema
    case InvalidDocument

  sealed trait SchemaValidation:
    def schemaId: SchemaId
    def action: Action

  case class Success(
      schemaId: SchemaId,
      action: Action,
      schema: Schema
  ) extends SchemaValidation

  case class Failure(
      schemaId: SchemaId,
      action: Action,
      cause: Cause,
      message: String
  ) extends SchemaValidation
