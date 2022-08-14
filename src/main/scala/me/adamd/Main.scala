package me.adamd

import cats.effect.{IOApp, IO, Resource, Sync}
import cats.syntax.flatMap._
import me.adamd.http._
import me.adamd.services._
import me.adamd.persistence._
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.SelfAwareStructuredLogger

object Main extends IOApp.Simple {

  given unsafeLogger: SelfAwareStructuredLogger[IO] =
    Slf4jLogger.getLogger[IO]

  def run: IO[Unit] =
    logInit() *> (for
      store   <- SchemaStore.resource[IO](DbConfig.default)
      service <- SchemaService.resource(store)
      routes  <- Routes.resource(service)(Validator.validateJson)
      _       <- HttpServer.resource[IO](HttpConfig.default, routes)
    yield ()).use(_ => IO.never)

  private def logInit[F[_]: Logger](): F[Unit] =
    Logger[F].info("Starting service-validator")
}
