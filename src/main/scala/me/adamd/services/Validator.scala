package me.adamd.services

import cats.syntax.either._
import me.adamd.domain.models._
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.SchemaVersion
import com.github.fge.jsonschema.main.{JsonValidator, JsonSchemaFactory}
import com.github.fge.jsonschema.cfg.ValidationConfiguration

import collection.JavaConverters._

object Validator:

  val SchemaValidator: JsonValidator =
    JsonSchemaFactory
      .newBuilder()
      .setValidationConfiguration(
        ValidationConfiguration
          .newBuilder()
          .setDefaultVersion(SchemaVersion.DRAFTV4)
          .freeze()
      )
      .freeze()
      .getValidator()

  def validateJson(schema: Schema, document: Document): Either[String, Unit] =
    Either.catchNonFatal {
      val report = SchemaValidator.validate(
        JsonLoader.fromString(schema.value.noSpaces),
        JsonLoader.fromString(document.value.noSpaces)
      )

      if report.isSuccess()
      then ().asRight
      else
        report
          .iterator()
          .asScala
          .toList
          .map(_.getMessage())
          .mkString(",")
          .asLeft
    }.leftMap(_.getMessage()).flatten
