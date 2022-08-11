package me.adamd

import cats.effect.{IOApp, IO, Resource, Sync}
import cats.syntax.flatMap._
import me.adamd.http._
import me.adamd.persistence.SchemaStore
import me.adamd.services.Validator._
import me.adamd.services.SchemaService
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.SelfAwareStructuredLogger

object Main extends IOApp.Simple {

  given unsafeLogger: SelfAwareStructuredLogger[IO] =
    Slf4jLogger.getLogger[IO]

  def run: IO[Unit] =
    logInit() *> (for
      schemaStore   <- SchemaStore.resource()
      schemaService <- SchemaService.resource(schemaStore)
      routes        <- Routes.resource(schemaService)(cleanJson, validateJson)
      _             <- HttpServer.resource[IO]("0.0.0.0", 8080, routes)
    yield ()).use(_ => IO.never)

  private def logInit[F[_]: Logger](): F[Unit] =
    Logger[F].info("Starting service-validator")
}
