package net.tixxit.contract
package web

import java.net.URL

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Try, Success, Failure }

import akka.actor.{ Actor, Props, ActorRefFactory }

import spray.util.actorSystem
import spray.routing._
import spray.http._
import spray.http.StatusCodes.{ OK, NotFound, BadRequest, InternalServerError,
                                MovedPermanently, Found }
import spray.http.MediaTypes._
import spray.http.HttpHeaders._
import spray.httpx.marshalling._

import argonaut._
import Argonaut._
import ArgonautMarshallers._

trait ShortenerService extends HttpService {
  private implicit def executionContext: ExecutionContext = actorSystem.dispatcher

  def shortener: Shortener[Future]

  def requireURL[A: Marshaller](key: String)
      (f: URL => RequestContext => Unit): RequestContext => Unit = { ctx =>
    shortener.expand(key) onComplete {
      case Success(Some(url)) => f(url)(ctx)
      case Success(None) => ctx.complete(NotFound)
      case Failure(e) => ctx.complete(InternalServerError)
    }
  }

  def meta(key: String): RequestContext => Unit =
    requireURL(key) { url => _.complete(Map("key" -> key, "url" -> url.toString).asJson) }

  def expand(key: String): RequestContext => Unit =
    requireURL(key) { url => _.redirect(Uri(url.toString), MovedPermanently) }

  private final def isHttp(url: URL): Boolean =
    url.getProtocol == "http" || url.getProtocol == "https"

  def parseURL(f: URL => RequestContext => Unit): String => RequestContext => Unit = { url0 =>
    Try(new URL(url0)) match {
      case Success(url) if isHttp(url) => f(url)
      case _ => _.complete(BadRequest)
    }
  }

  def shorten: String => RequestContext => Unit = parseURL { url => ctx =>
    shortener.shorten(url) onComplete {
      case Success(key) => ctx.redirect(Uri(s"/$key/meta"), Found)
      case Failure(e) => ctx.complete(InternalServerError)
    }
  }

  def postWithUrl = post & formField('url.as[String])

  val uiRoute = 
    path("") {
      getFromResource("web/index.html", `text/html`)
    } ~
    pathPrefix("static" ~ Slash) {
      getFromResourceDirectory("web/static")
    }

  val shortenRoute =
    path("") {
      postWithUrl(shorten)
    } ~
    path(Segment / "meta") { key =>
      get(meta(key))
    } ~
    path(Segment) { key =>
      get(expand(key))
    }
}

final class ShortenerServiceActor(val shortener: Shortener[Future])
    extends HttpServiceActor with ShortenerService {
  def receive = runRoute(uiRoute ~ shortenRoute)
}

object ShortenerServiceActor {
  def props(shortener: Shortener[Future]) =
    Props(classOf[ShortenerServiceActor], shortener)
}
