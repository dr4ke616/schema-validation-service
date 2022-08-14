import Dependencies._
import com.typesafe.sbt.packager.docker._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "me.adamd"
ThisBuild / scalaVersion := "3.1.3"

val dockerSettings = Seq(
  Docker / version := version.value,
  Docker / daemonUser := "app",
  dockerExposedPorts := Seq(8080),
  dockerBaseImage := "openjdk:11-jre-slim-buster",
  dockerChmodType := DockerChmodType.UserGroupWriteExecute,
  dockerEnvVars := Map(
    "BACKEND"     -> "sqlite",
    "HTTP_PORT"   -> "8080",
    "SQLITE_FILE" -> "/opt/docker/bin/validator.db"
  )
)

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(dockerSettings)
  .settings(
    name := "schema-validation-service",
    scalacOptions ++= Seq(
      "-indent",
      "-new-syntax",
      "-Ycheck-mods",
      "-Ycheck-all-patmat"
    ),
    libraryDependencies ++=
      cats ++ catsEffect ++ db ++ http4s ++
        circe ++ jsonSchema ++ logging ++ unittest,
    Compile / mainClass := Some("me.adamd.App")
  )
