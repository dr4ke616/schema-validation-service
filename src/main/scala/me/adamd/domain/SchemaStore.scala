package me.adamd.domain

import cats.Monad
import cats.effect.Resource
import me.adamd.domain.models._
import me.adamd.domain.models.types._

trait SchemaStore[F[+_]]:
  def upsert(schemaId: SchemaId, schema: Schema, requestId: RequestId): F[Unit]
  def read(schemaId: SchemaId, requestId: RequestId): F[Option[Schema]]
