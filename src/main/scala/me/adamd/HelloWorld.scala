package me.adamd

import cats.effect.IO
import cats.Applicative

trait HelloWorld[F[_]]:
  def say(): F[String]

class HelloWorldImpl[F[_]: Applicative] extends HelloWorld[F]:
  override def say(): F[String] = Applicative[F].pure("Hello Cats!")

object HelloWorld:
  def apply[F[_]: Applicative]() = new HelloWorldImpl[F]()
