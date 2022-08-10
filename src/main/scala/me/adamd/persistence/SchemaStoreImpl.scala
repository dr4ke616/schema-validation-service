package me.adamd.persistence

import me.adamd.domain.SchemaStore

class SchemaStoreImpl[F[_]]() extends SchemaStore[F]:
  def read(): F[Unit]   = ???
  def upsert(): F[Unit] = ???
