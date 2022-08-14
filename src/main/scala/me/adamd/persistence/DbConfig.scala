package me.adamd.persistence

import cats.syntax.option._

sealed trait DbConfig

case object InMmeDbConfig extends DbConfig

final case class SqliteDbConfig(
    file: String,
    table: String,
    user: Option[String] = none,
    pass: Option[String] = none
) extends DbConfig
