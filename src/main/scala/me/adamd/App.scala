package me.adamd

import cats.effect.{IOApp, IO, Resource, Sync}
import cats.syntax.flatMap._
import me.adamd.http._
import me.adamd.services._
import me.adamd.persistence._
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.SelfAwareStructuredLogger
import scala.util.Try

object App extends IOApp.Simple {

  given unsafeLogger: SelfAwareStructuredLogger[IO] =
    Slf4jLogger.getLogger[IO]

  def run: IO[Unit] =
    logInit() *> (for
      store   <- SchemaStore.resource[IO](EnvConfig.db)
      service <- SchemaService.resource(store)
      routes  <- Routes.resource(service)(Validator.validateJson)
      _       <- HttpServer.resource[IO](EnvConfig.http, routes)
    yield ()).use(_ => IO.never)

  private def logInit[F[_]: Logger](): F[Unit] =
    Logger[F].info("Starting service-validator")
}

private object EnvConfig:
  import sys.env

  private lazy val port: Int =
    sys.env
      .get("HTTP_PORT")
      .flatMap(x => Try(x.toInt).toOption)
      .getOrElse(8080)

  private lazy val host: String =
    sys.env
      .get("HTTP_HOST")
      .map(_.toLowerCase().trim())
      .getOrElse("0.0.0.0")

  private lazy val backend: Option[String] =
    sys.env
      .get("BACKEND")
      .map(_.toLowerCase().trim())

  private lazy val sqliteFile: String =
    sys.env
      .get("SQLITE_FILE")
      .map(_.toLowerCase().trim())
      .getOrElse("validator.db")

  lazy val http: HttpConfig =
    HttpConfig(host, port)

  lazy val db: DbConfig = backend match {
    case Some("in-mem") => InMmeDbConfig
    case Some("sqlite") | _ => // default
      SqliteDbConfig(file = sqliteFile, table = "json_schema")
  }
