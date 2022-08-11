package me.adamd.services

import cats.Monad
import me.adamd.domain.models._

object Validator:

  def cleanJson[F[+_]: Monad](document: Document): F[Document] = ???

  def validateJson[F[+_]](
      schema: Schema,
      document: Document
  ): F[SchemaValidation] = ???
