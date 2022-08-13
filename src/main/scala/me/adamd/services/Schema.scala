package me.adamd.services

import cats.Monad
import cats.effect.Resource
import cats.syntax.functor._
import cats.syntax.option._
import cats.syntax.either._
import me.adamd.domain._
import me.adamd.domain.models._
import me.adamd.domain.models.types._

trait SchemaService[F[+_]]:

  def upsert(
      schemaId: SchemaId,
      schema: Schema,
      requestId: RequestId
  ): F[SchemaValidation]

  def read(
      schemaId: SchemaId,
      requestId: RequestId
  ): F[SchemaValidation]

object SchemaService:

  def apply[F[+_]: Monad](store: SchemaStore[F]) = new SchemaService[F]:

    override def upsert(
        schemaId: SchemaId,
        schema: Schema,
        requestId: RequestId
    ): F[SchemaValidation] =
      val ok = Success(schemaId, Action.UploadSchema, schema)
      store.upsert(schemaId, schema, requestId).as(ok)

    override def read(
        schemaId: SchemaId,
        requestId: RequestId
    ): F[SchemaValidation] =
      val notFound =
        Failure(schemaId, Action.GetSchema, Cause.NonExist, "schema not found")
      val ok: Schema => SchemaValidation =
        Success(schemaId, Action.GetSchema, _)
      store.read(schemaId, requestId).map(_.fold(notFound)(ok))

  def resource[F[+_]: Monad](
      store: SchemaStore[F]
  ): Resource[F, SchemaService[F]] =
    Resource.pure(apply(store))
