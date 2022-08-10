import Dependencies._

lazy val root = (project in file("."))
  .settings(
    name := "validation-service",
    organization := "me.adamd",
    scalaVersion := "3.1.3",
    // crossScalaVersions := Seq("3.1.3"),
    libraryDependencies ++=
      catsEffect ++ cats ++ circe ++ http4s :+ jsonSchema
  )
