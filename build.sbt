import Dependencies._

lazy val root = (project in file("."))
  .settings(
    name := "schema-validation-service",
    organization := "me.adamd",
    scalaVersion := "3.1.3",
    libraryDependencies ++=
      cats ++ catsEffect ++ db ++ http4s ++
        circe ++ logging ++ unittest :+ jsonSchema
  )
