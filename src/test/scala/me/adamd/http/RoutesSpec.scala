package me.adamd.http

import munit._
import org.http4s._
import cats.effect.IO
import cats.syntax.option._
import cats.syntax.applicative._
import io.circe.Json
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.circe.CirceEntityDecoder._
import me.adamd.services._
import me.adamd.domain.models._
import me.adamd.domain.models.types._

class RoutesSpec extends CatsEffectSuite {

  test("GET /schema/<id> - 200") {
    val schema  = Schema(Map("foo" -> "bar").asJson)
    val success = Success(SchemaId.apply("id-1"), Action.GetSchema, schema)
    val route =
      Routes.apply(mock(success))(_.pure[IO], (_, _) => success.pure[IO])

    val resp = route.orNotFound.run(
      Request(method = Method.GET, uri = Uri.unsafeFromString("/schema/id-1"))
    )

    val expected = schema.value
    assertEquals(check(resp, Status.Ok, expected.some), true)
  }

  test("GET /schema/<id> - 404") {
    val fail =
      Failure(SchemaId.apply("id-1"), Action.GetSchema, Cause.NotFound, "na")
    val route =
      Routes.apply(mock(fail))(_.pure[IO], (_, _) => fail.pure[IO])

    val resp = route.orNotFound.run(
      Request(method = Method.GET, uri = Uri.unsafeFromString("/schema/id-1"))
    )

    val expected = Map(
      "id"      -> "id-1",
      "status"  -> "error",
      "action"  -> "getSchema",
      "message" -> "na"
    ).asJson
    assertEquals(check(resp, Status.NotFound, expected.some), true)
  }

  test("POST /schema/<id> - 201") {
    val schema  = Schema(Map("foo" -> "bar").asJson)
    val success = Success(SchemaId.apply("id-1"), Action.UploadSchema, schema)
    val route =
      Routes.apply(mock(success))(_.pure[IO], (_, _) => success.pure[IO])

    val resp = route.orNotFound.run(
      Request(method = Method.POST, uri = Uri.unsafeFromString("/schema/id-1"))
        .withEntity(schema.value)
    )

    val expected = Map(
      "id"     -> "id-1",
      "status" -> "success",
      "action" -> "uploadSchema"
    ).asJson
    assertEquals(check(resp, Status.Created, expected.some), true)
  }

  test("POST /schema/<id> - 400") {
    val schema = Schema(Map("foo" -> "bar").asJson)
    val fail = Failure(
      SchemaId.apply("id-1"),
      Action.UploadSchema,
      Cause.InvalidSchema,
      "Invalid JSON"
    )
    val route = Routes.apply(mock(fail))(_.pure[IO], (_, _) => fail.pure[IO])

    val resp = route.orNotFound.run(
      Request(method = Method.POST, uri = Uri.unsafeFromString("/schema/id-1"))
        .withEntity(schema.value)
    )

    val expected = Map(
      "id"      -> "id-1",
      "status"  -> "error",
      "action"  -> "uploadSchema",
      "message" -> "Invalid JSON"
    ).asJson
    assertEquals(check[Json](resp, Status.BadRequest, Some(expected)), true)
  }

  test("POST /validate/<id> - 200") {
    val schema = Schema(Map("foo" -> "bar").asJson)
    val body   = Document(Map("baz" -> "bar").asJson)
    val success =
      Success(SchemaId.apply("id-1"), Action.ValidateDocument, schema)
    val route =
      Routes.apply(mock(success))(_.pure[IO], (_, _) => success.pure[IO])

    val resp = route.orNotFound.run(
      Request(
        method = Method.POST,
        uri = Uri.unsafeFromString("/validate/id-1")
      ).withEntity(body.value)
    )

    val expected = body.value
    assertEquals(check[Json](resp, Status.Ok, expected.some), true)
  }

  test("POST /validate/<id> - 400") {
    val schema = Schema(Map("foo" -> "bar").asJson)
    val body   = Document(Map("baz" -> "bar").asJson)
    val fail =
      Failure(
        SchemaId.apply("id-1"),
        Action.ValidateDocument,
        Cause.InvalidDocument,
        "Property '/root/timeout' is required"
      )
    val route =
      Routes.apply(mock(fail))(_.pure[IO], (_, _) => fail.pure[IO])

    val resp = route.orNotFound.run(
      Request(
        method = Method.POST,
        uri = Uri.unsafeFromString("/validate/id-1")
      ).withEntity(body.value)
    )

    val expected = Map(
      "id"      -> "id-1",
      "status"  -> "error",
      "action"  -> "validateDocument",
      "message" -> "Property '/root/timeout' is required"
    ).asJson
    assertEquals(check[Json](resp, Status.BadRequest, expected.some), true)
  }

  private def check[A](
      actual: IO[Response[IO]],
      expectedStatus: Status,
      expectedBody: Option[A]
  )(using ev: EntityDecoder[IO, A]): Boolean =
    val actualResp  = actual.unsafeRunSync()
    val statusCheck = actualResp.status == expectedStatus
    val bodyCheck = expectedBody.fold[Boolean](
      actualResp.body.compile.toVector.unsafeRunSync().isEmpty
    )(_ == actualResp.as[A].unsafeRunSync())
    statusCheck && bodyCheck

  private def mock(v: SchemaValidation) =
    new SchemaService[IO]:

      override def upsert(
          schemaId: SchemaId,
          schema: Schema,
          requestId: RequestId
      ): IO[SchemaValidation] = v.pure[IO]

      override def read(
          schemaId: SchemaId,
          requestId: RequestId
      ): IO[SchemaValidation] = v.pure[IO]
}
