package me.adamd.http

import cats.data.EitherT
import cats.effect.{Resource, Async}
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicative._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.circe.CirceEntityDecoder._
import org.typelevel.ci._
import me.adamd.domain.JsonCodecs
import me.adamd.domain.models._
import me.adamd.domain.models.Cause._
import me.adamd.domain.models.Action._
import me.adamd.domain.models.types._
import me.adamd.services.SchemaService
import io.circe.syntax._
import io.circe.Json

object Routes extends JsonCodecs:

  def resource[F[+_]: Async](schemaService: SchemaService[F])(
      validateDocument: (Schema, Document) => Either[String, Unit]
  ): Resource[F, HttpRoutes[F]] =
    Resource.pure(apply[F](schemaService)(validateDocument))

  def apply[F[+_]: Async](schemaService: SchemaService[F])(
      validateDocument: (Schema, Document) => Either[String, Unit]
  ): HttpRoutes[F] = HttpRoutes.of[F] {

    case req @ GET -> Root / "schema" / schemaId =>
      val (rid, sid) = (requestId(req), SchemaId.apply(schemaId))

      schemaService.read(sid, rid).map {
        case x: Success => respond(Status.Ok, x.schema.asJson)
        case x: Failure => fail(x)
      }

    case req @ POST -> Root / "schema" / schemaId =>
      val (rid, sid) = (requestId(req), SchemaId.apply(schemaId))

      val decode: EitherT[F, SchemaValidation, Schema] =
        req.attemptAs[Schema].leftMap { e =>
          Failure(sid, UploadSchema, InvalidSchema, e.getMessage)
        }

      val store: Schema => EitherT[F, SchemaValidation, SchemaValidation] =
        x => EitherT(schemaService.upsert(sid, x, rid).map(_.asRight))

      (decode >>= store).merge.map {
        case x: Success => respond(Status.Created, x.asJson)
        case x: Failure => fail(x)
      }

    case req @ POST -> Root / "validate" / schemaId =>
      type FailOr[A] = EitherT[F, Failure, A]
      val (rid, sid) = (requestId(req), SchemaId.apply(schemaId))

      val decodeBody: FailOr[Document] =
        req.attemptAs[Document].leftMap { e =>
          Failure(sid, ValidateDocument, InvalidDocument, e.getMessage)
        }

      val readSchema: FailOr[Schema] =
        EitherT(schemaService.read(sid, rid).map {
          case x: Failure => x.asLeft[Schema]
          case x: Success => x.schema.asRight
        })

      val validate: (Document, Schema) => FailOr[Document] = (d, s) =>
        EitherT(
          validateDocument(s, d)
            .bimap(Failure(sid, ValidateDocument, InvalidSchema, _), _ => d)
            .pure[F]
        )

      (decodeBody, readSchema).flatMapN(validate(_, _)).value.map {
        _.map(_.asJson).fold(fail, respond(Status.Ok, _))
      }
  }

  private def requestId[F[+_]: Async](req: Request[F]): RequestId =
    RequestId.apply(
      req.headers.get(ci"X-Request-ID").fold("null")(_.head.value)
    )

  private def fail[F[_]: Async](f: Failure): Response[F] = f match {
    case x @ Failure(_, _, NonExist, _) =>
      respond(Status.NotFound, x.asJson)
    case x @ Failure(_, _, InvalidDocument | InvalidSchema, _) =>
      respond(Status.BadRequest, x.asJson)
  }

  private def respond[F[_]: Async](status: Status, body: Json): Response[F] =
    Response[F](status).withEntity(body)
