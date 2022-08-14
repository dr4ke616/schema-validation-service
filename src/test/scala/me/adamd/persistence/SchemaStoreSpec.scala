package me.adamd.persistence

import java.util.UUID
import java.nio.file._

import munit._
import cats.effect.IO
import cats.syntax.option._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.SelfAwareStructuredLogger
import io.circe.syntax._
import me.adamd.domain.models._
import me.adamd.domain.models.types._

class SchemaStoreSpec extends CatsEffectSuite {

  private given unsafeLogger: SelfAwareStructuredLogger[IO] =
    Slf4jLogger.getLogger[IO]

  private val files = FunFixture[Path](
    setup = _ =>
      Files.createTempFile(s"tmp-schema-store-spec", s"SchemaStoreSpec.db"),
    teardown = Files.deleteIfExists
  )

  files.test("read *> write *> read") { file =>
    val c = SqliteDbConfig(
      file = file.toFile().getAbsolutePath(),
      table = "json_schema"
    )

    SchemaStore
      .resource[IO](c)
      .use { svc =>
        val sid    = SchemaId.apply(UUID.randomUUID().toString())
        val rid    = RequestId.apply(UUID.randomUUID().toString())
        val schema = Schema(Map("foo" -> "bar").asJson)

        assertIO(svc.read(sid, rid), none[Schema]) *>
          assertIO(svc.upsert(sid, schema, rid), ()) *>
          assertIO(svc.read(sid, rid), schema.some)
      }
      .unsafeRunSync()
  }
}
