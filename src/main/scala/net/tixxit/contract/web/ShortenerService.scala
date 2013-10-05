package net.tixxit.contract
package web

import java.net.URL

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Try, Success, Failure }

import akka.actor.{ Actor, Props }
import spray.routing._
import spray.http._
import spray.http.StatusCodes.{ NotFound, BadRequest, InternalServerError,
                                MovedPermanently, TemporaryRedirect }
import MediaTypes._

final class ShortenerServiceActor(val shortener: Shortener[Future])
    extends HttpServiceActor with ShortenerService {
  def executionContext = context.dispatcher
  def receive = runRoute(shortenRoute)
}

object ShortenerServiceActor {
  def props(shortener: Shortener[Future]) =
    Props(classOf[ShortenerServiceActor], shortener)
}

trait ShortenerService extends HttpService {
  implicit def executionContext: ExecutionContext

  def shortener: Shortener[Future]

  def expand(key: String): RequestContext => Unit = { ctx =>
    shortener.expand(key) onComplete {
      case Success(Some(url)) => ctx.redirect(Uri(url.toString), MovedPermanently)
      case Success(None) => ctx.complete(NotFound)
      case Failure(e) => ctx.complete(InternalServerError)
    }
  }

  def shorten: String => RequestContext => Unit = { url0 => ctx =>
    Try(new URL(url0)) match {
      case Success(url) =>
        shortener.shorten(url) onComplete {
          case Success(key) => ctx.redirect(Uri(key), TemporaryRedirect)
          case Failure(e) => ctx.complete(InternalServerError)
        }

      case Failure(e) =>
        ctx.complete(BadRequest)
    }
  }

  val shortenRoute =
    path("") {
      (post & parameter('url.as[String]))(shorten)
      get {
        respondWithMediaType(`text/html`) { // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            <html>
              <body>
                <h1>Say hello to <i>spray-routing</i> on <i>spray-can</i>!</h1>
              </body>
            </html>
          }
        }
      }
    } ~
    path(Segment) { key =>
      get(expand(key))
    }
}
