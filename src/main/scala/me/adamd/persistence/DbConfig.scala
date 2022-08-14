package me.adamd.persistence

sealed trait DbConfig

case object InMmeDbConfig extends DbConfig

final case class SqliteDbConfig(
    user: String,
    pass: String,
    file: String,
    table: String
) extends DbConfig

object DbConfig:
  val default = SqliteDbConfig("", "", "validator.db", "json_schema")
