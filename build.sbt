import Dependencies._

lazy val core = cats ++ catsEffect ++ db ++ http4s
lazy val json = circe ++ jsonSchema

lazy val root = (project in file("."))
  .settings(
    name := "schema-validation-service",
    organization := "me.adamd",
    scalaVersion := "3.1.3",
    scalacOptions ++= Seq(
      // "-explain",
      "-indent",
      "-new-syntax",
      "-Ycheck-mods",
      "-Ycheck-all-patmat"
    ),
    libraryDependencies ++=
      core ++ json ++ logging ++ unittest
  )
