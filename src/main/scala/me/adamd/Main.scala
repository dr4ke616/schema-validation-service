package me.adamd

import cats.effect.{IOApp, IO}

object Main extends IOApp.Simple {

  def run: IO[Unit] =
    HelloWorld[IO]().say().flatMap(IO.println)
}
