package me.adamd.http

final case class HttpConfig(host: String, port: Int)

object HttpConfig:
  val default = HttpConfig("0.0.0.0", 8080)
