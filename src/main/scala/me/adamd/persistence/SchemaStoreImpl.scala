package me.adamd.persistence

import cats.effect.{Resource, Async}
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.functor._
import me.adamd.domain.SchemaStore
import me.adamd.domain.models._
import me.adamd.domain.models.types._
import me.adamd.persistence.SchemaStore.logAround
import doobie._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import doobie.implicits._
import io.circe.parser.parse
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.SelfAwareStructuredLogger

import scala.collection.mutable.HashMap

class MemSchemaStore[F[+_]: Async: Logger]() extends SchemaStore[F]:
  private val store: HashMap[SchemaId, Schema] = HashMap.empty

  override def upsert(
      schemaId: SchemaId,
      schema: Schema,
      requestId: RequestId
  ): F[Unit] =
    Async[F]
      .delay(store.put(schemaId, schema))
      .void
      .logAround(requestId, s"[schema_id=$schemaId] upserting")

  override def read(
      schemaId: SchemaId,
      requestId: RequestId
  ): F[Option[Schema]] =
    Async[F]
      .delay(store.get(schemaId))
      .logAround(requestId, s"[schema_id=$schemaId] reading")

class SqliteSchemaStore[F[+_]: Async: Logger](
    transactor: Transactor[F],
    table: String
) extends SchemaStore[F]:

  private val Table = Fragment.const(table)

  override def upsert(
      schemaId: SchemaId,
      schema: Schema,
      requestId: RequestId
  ): F[Unit] =
    sql"""
      INSERT INTO $Table (schema_id, schema)
      VALUES(${schemaId.value}, ${schema.value.noSpaces})
      ON CONFLICT(schema_id) DO UPDATE SET schema=${schema.value.noSpaces}
    """.update.run
      .transact(transactor)
      .void
      .logAround(requestId, s"[schema_id=$schemaId] upserting")

  override def read(
      schemaId: SchemaId,
      requestId: RequestId
  ): F[Option[Schema]] =
    sql"SELECT schema FROM $Table WHERE schema_id=${schemaId.value}"
      .query[String]
      .option
      .map(_.map(parse(_).leftMap(_.message).fold(sys.error, Schema.apply)))
      .transact(transactor)
      .logAround(requestId, s"[schema_id=$schemaId] reading")

object SchemaStore:

  def resource[F[+_]: Async: Logger](
      config: DbConfig
  ): Resource[F, SchemaStore[F]] =
    given unsafeLogger: SelfAwareStructuredLogger[F] =
      Slf4jLogger.getLogger[F]

    config match {
      case InMmeDbConfig              => memResource()
      case SqliteDbConfig(f, t, u, p) => sqliteResource(f, t, u, p)
    }

  private def memResource[F[+_]: Async: Logger](): Resource[F, SchemaStore[F]] =
    Resource.eval(Logger[F].info("Bootstrapping in-mem store")) *>
      Resource.pure(new MemSchemaStore[F]())

  private def sqliteResource[F[+_]: Async: Logger](
      file: String,
      table: String,
      user: Option[String],
      pass: Option[String]
  ): Resource[F, SchemaStore[F]] =
    def createTable(tx: Transactor[F]) =
      val action = sql"""
        CREATE TABLE IF NOT EXISTS ${Fragment.const(table)} (
          schema_id STRING NOT NULL PRIMARY KEY,
          schema TEXT NOT NULL
        )
        """.update.run
        .transact(tx)
        .void
      Logger[F].info(s"creating table $table") *> action

    Resource.eval(Logger[F].info("Bootstrapping sqlite store")) *>
      (for
        ex <- ExecutionContexts.fixedThreadPool(32)
        tx <- HikariTransactor.newHikariTransactor[F](
                "org.sqlite.JDBC",
                s"jdbc:sqlite:$file",
                user.getOrElse(""),
                pass.getOrElse(""),
                ex
              )
        _ <- Resource.eval(createTable(tx))
      yield new SqliteSchemaStore[F](tx, table))

  extension [F[_]: Async: Logger, A](fa: F[A])

    private[persistence] def logAround(
        requestId: RequestId,
        msg: String
    ): F[A] =
      Logger[F].info(s"[request_id=$requestId] $msg...") *> fa
        <* Logger[F].info(s"[request_id=$requestId] $msg Done!")
