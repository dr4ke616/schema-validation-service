import sbt._

object Dependencies {

  val CatsVersion        = "2.8.0"
  val CatsEffectVersion  = "3.3.14"
  val CatsLoggingVersion = "2.4.0"
  val CirceVersion       = "0.14.2"
  val Http4sVersion      = "1.0.0-M35"

  val circe = Seq(
    "io.circe" %% "circe-core"    % CirceVersion,
    "io.circe" %% "circe-generic" % CirceVersion,
    "io.circe" %% "circe-parser"  % CirceVersion
  )

  val cats = Seq(
    "org.typelevel" %% "cats-core" % CatsVersion
  )

  val catsEffect = Seq(
    "org.typelevel" %% "cats-effect"        % CatsEffectVersion,
    "org.typelevel" %% "cats-effect-kernel" % CatsEffectVersion
  )

  val http4s = Seq(
    "org.http4s" %% "http4s-dsl"          % Http4sVersion,
    "org.http4s" %% "http4s-circe"        % Http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % Http4sVersion
    // "org.http4s" %% "http4s-blaze-client" % Http4sVersion
  )

  val jsonSchema =
    "com.github.java-json-tools" % "json-schema-validator" % "2.2.14"

  val logging = Seq(
    "org.typelevel" %% "log4cats-core"   % CatsLoggingVersion,
    "org.typelevel" %% "log4cats-slf4j"  % CatsLoggingVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.11"
  )

  val unittest = Seq(
    "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
    // "com.alejandrohdezma" %% "http4s-munit"        % "0.11.0" % Test
  )
}
