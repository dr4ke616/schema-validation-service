package me.adamd.persistence

import cats.Monad
import cats.effect.Resource
import me.adamd.domain.SchemaStore
import me.adamd.domain.models._
import me.adamd.domain.models.types._
import scala.collection.mutable.HashMap

class MemSchemaStore[F[+_]: Monad]() extends SchemaStore[F]:
  private val store: HashMap[SchemaId, Schema] = HashMap.empty

  override def upsert(
      schemaId: SchemaId,
      schema: Schema,
      requestId: RequestId
  ): F[Unit] =
    Monad[F].pure(store.put(schemaId, schema))

  override def read(
      schemaId: SchemaId,
      requestId: RequestId
  ): F[Option[Schema]] =
    Monad[F].pure(store.get(schemaId))

class SqliteSchemaStore[F[+_]: Monad]() extends SchemaStore[F]:

  override def upsert(
      schemaId: SchemaId,
      schema: Schema,
      requestId: RequestId
  ): F[Unit] =
    ???

  override def read(
      schemaId: SchemaId,
      requestId: RequestId
  ): F[Option[Schema]] = ???

object SchemaStore:

  def apply[F[+_]: Monad](): SchemaStore[F] =
    new MemSchemaStore[F]()

  def resource[F[+_]: Monad](): Resource[F, SchemaStore[F]] =
    Resource.pure(apply())
