package me.adamd.services

trait ValidatorService[F[_]]:
  def validate(): F[Unit]

object ValidatorService:

  def apply[F[_]]() = new ValidatorService[F]:
    def validate(): F[Unit] = ???
