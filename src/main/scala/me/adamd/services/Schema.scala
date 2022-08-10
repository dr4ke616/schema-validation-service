package me.adamd.services

import me.adamd.domain._

trait SchemaService[F[_]]:
  def upsert(): F[Unit]
  def read(): F[Unit]

object SchemaService:

  def apply[F[_]](store: SchemaStore[F]) = new SchemaService[F]:
    def read(): F[Unit]   = store.read()
    def upsert(): F[Unit] = store.upsert()
