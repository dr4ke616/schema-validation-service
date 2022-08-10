package me.adamd.domain

trait SchemaStore[F[_]]:
  def upsert(): F[Unit]
  def read(): F[Unit]
