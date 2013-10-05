package net.tixxit.contract
package store

import scala.concurrent.{ ExecutionContext, Future }

import java.net.URL

import akka.actor.{ ActorSystem, ActorRef, Actor, Props }
import akka.routing.RoundRobinRouter
import akka.util.Timeout

import scalaz.effect.IO

import akka.pattern.ask

case class PutIfAbsent(key: Array[Byte], value: Array[Byte])
case class PutResult(key: Array[Byte], oldValue: Option[Array[Byte]])

case class Get(key: Array[Byte])
case class GetResult(key: Array[Byte], value: Option[Array[Byte]])

case object Close

final class IOStoreActor(val store: Store[IO]) extends Actor {
  def receive = {
    case PutIfAbsent(key, value) =>
      sender ! PutResult(key, store.putIfAbsent(key, value).unsafePerformIO)

    case Get(key) =>
      sender ! GetResult(key, store.get(key).unsafePerformIO)

    case Close =>
      store.close.unsafePerformIO
  }
}

object IOStoreActor {
  def props(store: Store[IO]) = Props(classOf[IOStoreActor], store)
}

/**
 * A `Store` that wraps an actor-backed implementation.
 */
final class ActorStore(system: ActorSystem, store: ActorRef)(implicit
    timeout: Timeout) extends Store[Future] {
  private implicit def ec = system.dispatcher

  def putIfAbsent(key: Array[Byte], value: Array[Byte]): Future[Option[Array[Byte]]] =
    (store ? PutIfAbsent(key, value)).mapTo[PutResult] map (_.oldValue)

  def get(key: Array[Byte]): Future[Option[Array[Byte]]] =
    (store ? Get(key)).mapTo[GetResult] map (_.value)

  def close(): Future[Unit] =
    (store ? Close) map (_ => ())
}

object ActorStore {
  def apply(n: Int)(store: => Store[IO])(implicit
      system: ActorSystem, to: Timeout): Store[Future] = {
    val actors = List.fill(n)(system.actorOf(IOStoreActor.props(store)))
    val router = system.actorOf(Props.empty.withRouter(RoundRobinRouter(routees = actors)))
    new ActorStore(system, router)
  }
}
