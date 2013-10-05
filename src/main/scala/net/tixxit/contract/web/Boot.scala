package net.tixxit.contract
package web

import scala.concurrent.duration.Duration
import scala.concurrent.Future

import java.io.File

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import akka.io.IO

import spray.can.Http

import scalaz.Monad

import scalaz.contrib.std.scalaFuture

object Boot extends App {
  implicit val system: ActorSystem = ActorSystem("contract-web")
  implicit val timeout: Timeout = Duration(5, "s")
  implicit val futureMonad: Monad[Future] = scalaFuture.futureInstance(system.dispatcher)

  val underlying = store.BerkeleyDBStore(new File("./db"), "contract-web")
  val urlStore = store.ActorStore(5)(underlying)
  val shortener = HashShortener(hash.MD5HashStream, urlStore)
  val service = system.actorOf(ShortenerServiceActor.props(shortener), "contract-web-service")

  IO(Http) ! Http.Bind(service, interface = "localhost", port = 8080)
}
