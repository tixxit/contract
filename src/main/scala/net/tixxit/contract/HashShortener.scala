package net.tixxit.contract

import java.net.URL
import java.util.Arrays

import scalaz.{ Monad, OptionT }
import scalaz.OptionT.optionT
import scalaz.syntax.monad._

import net.tixxit.contract.util._
import net.tixxit.contract.hash.HashStream
import net.tixxit.contract.store.Store

/**
 * A [[Shortener]] that requires a [[HashStream]] and a [[Store]]. The hash
 * stream is used to compute a stream of possible "shortenings" of a URL. We
 * try to associate the URL with each hash, in order, until our first success,
 * which is then used as the shortened value. Since the `HashStream` should
 * produce deterministics results, and the [[Store#putIfAbsent]] method must
 * be atomic, we are guaranteed that `shorten` will be idempotent.
 */
final case class HashShortener[M[+_]: Monad](hasher: HashStream, store: Store[M]) extends Shortener[M] {
  def shorten(url: URL): M[String] = {
    val url0 = url.toString
    put(hasher.hashStream(url0), url0.getBytes(Charsets.Utf8))
  }

  def expand(key: String): M[Option[URL]] =
    (optionT[M](store.get(key.getBytes(Charsets.Utf8))) map { value =>
      new URL(new String(value, Charsets.Utf8))
    }).run

  private def put(hashes: InfStream[String], value: Array[Byte]): M[String] = {
    val key = hashes.head.getBytes(Charsets.Utf8)
    store.putIfAbsent(key, value) flatMap { oldValue =>
      oldValue filter { xs =>
        !Arrays.equals(value, xs)
      } map { _ =>
        put(hashes.tail, value)
      } getOrElse {
        Monad[M].point(hashes.head)
      }
    }
  }
}
