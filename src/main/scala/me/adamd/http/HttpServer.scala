package me.adamd.http

import cats.effect.{Async, Resource}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.{HttpRoutes, Response, Status}
import org.http4s.server.{Server, Router}
import org.http4s.server.middleware.{Logger, RequestId}

object HttpServer:

  def resource[F[+_]: Async](
      config: HttpConfig,
      routes: HttpRoutes[F]
  ): Resource[F, Server] =
    BlazeServerBuilder[F]
      .bindHttp(config.port, config.host)
      .withHttpApp(middleware(routes).orNotFound)
      .resource

  private def middleware[F[_]: Async](routes: HttpRoutes[F]): HttpRoutes[F] =
    Logger.httpRoutes(logHeaders = true, logBody = false)(
      RequestId.httpRoutes(routes)
    )
