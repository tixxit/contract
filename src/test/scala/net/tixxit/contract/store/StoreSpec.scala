package net.tixxit.contract
package store

import net.tixxit.contract.util._

import scala.concurrent.Future
import scala.concurrent.duration.Duration

import java.io.File

import org.specs2.mutable.Specification

import akka.actor.ActorSystem
import akka.util.Timeout

import scalaz._
import scalaz.effect._

import scalaz.syntax.monad._
import scalaz.contrib.std.scalaFuture._

trait StoreSpec[M[+_]] extends Specification {
  implicit def M: Monad[M]
  def coM: Comonad[M]

  def withStore[A](f: Store[M] => A): A

  private def l(x: Option[Array[Byte]]): Option[Seq[Byte]] = x map (_.toList)

  "Store" should {
    "return None for get of non-existant key" in {
      withStore { store =>
        coM.copoint(store.get(Array(0))) must_== None
        coM.copoint(store.get(Array(1, 2, 3))) must_== None
      }
    }

    "put of non-existent key should be successful" in {
      withStore { store =>
        def checkPutGet(key: Array[Byte], value: Array[Byte]) = coM.copoint(for {
          putResult0 <- store.putIfAbsent(key, value)
          getResult0 <- store.get(key)
        } yield {
          (putResult0, l(getResult0))
        }) must_== (None, l(Some(value)))

        checkPutGet(Array(0), Array(1))
        checkPutGet(Array(1, 2, 3), Array(4, 5, 6))
      }
    }

    "put of existing key should return old value" in {
      withStore { store =>
        val key1 = Array[Byte](1, 2, 3)
        val key2 = Array[Byte](4, 5, 6)

        val (pr1, gr1, pr2, gr2) = coM.copoint(for {
          pr1 <- store.putIfAbsent(key1, Array(4, 2))
          gr1 <- store.get(key1)
          pr2 <- store.putIfAbsent(key1, Array(2, 3))
          gr2 <- store.get(key1)
        } yield (l(pr1), l(gr1), l(pr2), l(gr2)))

        pr1 must_== None
        gr1 must_== Some(Seq(4, 2))
        pr2 must_== Some(Seq(4, 2))
        gr2 must_== Some(Seq(4, 2))
      }
    }
  }
}

class InMemoryStoreSpec extends StoreSpec[Need] {
  val M = Need.need
  val coM = Need.need

  def withStore[A](f: Store[Need] => A): A = f(new InMemoryStore[Need])
}

class BerkeleyDBStoreSpec extends StoreSpec[IO] {
  val M = Monad[IO]
  val coM = IOComonad

  def withStore[A](f: Store[IO] => A): A = {
    val dir = File.createTempFile("StoreSpec", "bdb")
    dir.delete()
    dir.mkdirs() // Not really safe, but... tests.

    val store = BerkeleyDBStore(dir, "store-spec")
    val result = f(store)
    store.close().unsafePerformIO
    result
  }
}

class ActorStoreSpec extends StoreSpec[Future] {
  implicit val system: ActorSystem = ActorSystem("store-spec")
  implicit def executionContext = system.dispatcher
  implicit val timeout = Timeout(Duration(5, "s"))

  val M = Monad[Future]
  val coM = new FutureComonad(timeout.duration)

  def withStore[A](f: Store[Future] => A): A = {
    val store = new InMemoryStore[IO]
    f(ActorStore(5)(store))
  }
}
