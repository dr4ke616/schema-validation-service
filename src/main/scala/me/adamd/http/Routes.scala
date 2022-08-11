package me.adamd.http

import cats.effect.{Resource, Async}
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.syntax._
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
import io.circe.JsonObject
import io.circe.Json
import org.http4s.DecodeFailure
import cats.data.EitherT
import me.adamd.services.Validator

object Routes extends JsonCodecs:

  def resource[F[+_]: Async](schemaService: SchemaService[F])(
      cleanDocument: (Document) => F[Document],
      validateDocument: (Schema, Document) => F[SchemaValidation]
  ): Resource[F, HttpRoutes[F]] =
    Resource.pure(apply[F](schemaService)(cleanDocument, validateDocument))

  def apply[F[+_]: Async](schemaService: SchemaService[F])(
      cleanDocument: (Document) => F[Document],
      validateDocument: (Schema, Document) => F[SchemaValidation]
  ): HttpRoutes[F] = HttpRoutes.of[F] {

    case req @ GET -> Root / "schema" / schemaId =>
      val (rid, sid) = (requestId(req), SchemaId.apply(schemaId))
      schemaService.read(sid, rid).map {
        case x: Success => resp(Status.Ok, x.schema.value)
        case x: Failure => resp(Status.NotFound, x.asJson)
      }

    case req @ POST -> Root / "schema" / schemaId =>
      val (rid, sid) = (requestId(req), SchemaId.apply(schemaId))

      val decode: EitherT[F, SchemaValidation, Schema] =
        req.attemptAs[JsonObject].map(x => Schema(x.asJson)).leftMap { e =>
          Failure(sid, UploadSchema, InvalidSchema, e.getMessage)
        }

      val store: Schema => EitherT[F, SchemaValidation, SchemaValidation] =
        x => EitherT(schemaService.upsert(sid, x, rid).map(_.asRight))

      (decode >>= store).merge.map {
        case x: Success => resp(Status.Created, x.asJson)
        case x: Failure => resp(Status.BadRequest, x.asJson)
      }

    case req @ POST -> Root / "validate" / schemaId =>
      type FailOr[A] = EitherT[F, Failure, A]

      val (rid, sid) = (requestId(req), SchemaId.apply(schemaId))

      val decodeBody: FailOr[Document] =
        req.attemptAs[JsonObject].map(x => Document(x.asJson)).leftMap { e =>
          Failure(sid, ValidateDocument, InvalidDocument, e.getMessage)
        }

      val readSchema: FailOr[Schema] =
        EitherT(schemaService.read(sid, rid).map {
          case x: Failure => x.asLeft[Schema]
          case x: Success => x.schema.asRight
        })

      val cleanAndValidate: (Document, Schema) => FailOr[Document] = (d, s) =>
        EitherT((cleanDocument(d) >>= (validateDocument(s, _))).map {
          case x: Failure => x.asLeft[Document]
          case _: Success => d.asRight
        })

      (decodeBody, readSchema).flatMapN(cleanAndValidate(_, _)).value.map {
        case Left(x @ Failure(_, _, Cause.NotFound, _)) =>
          resp(Status.NotFound, x.asJson)
        case Left(x: Failure) => resp(Status.BadRequest, x.asJson)
        case Right(x)         => resp(Status.Ok, x.value)
      }
  }

  private def requestId[F[+_]: Async](req: Request[F]): RequestId =
    RequestId.apply(
      req.headers.get(ci"X-Request-ID").fold("null")(_.head.value)
    )

  private def resp[F[_]: Async](s: Status, j: Json): Response[F] =
    Response[F](s).withEntity(j)
