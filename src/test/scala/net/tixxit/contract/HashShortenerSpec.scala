package net.tixxit.contract

import net.tixxit.contract.util._
import net.tixxit.contract.hash._
import net.tixxit.contract.store._

import java.net.URL

import org.specs2.mutable.Specification

import scalaz._

class HashShortenerSpec extends ShortenerSpec[Need] {
  implicit def PrefixedHashStreamOps(stream: HashStream) = new {
    def ::(head: String): HashStream = new HashStream {
      def hashStream(value: String): InfStream[String] =
        InfStream(head, stream.hashStream(value))
    }
  }

  val coM: Comonad[Need] = Need.need

  def withShortener[A](f: Shortener[Need] => A): A =
    f(HashShortener(MD5HashStream, new InMemoryStore[Need]))

  "HashShortener" should {
    "collision are skipped" in {
      val stream = "a" :: "b" :: "c" :: MD5HashStream
      val shortener = HashShortener(stream, new InMemoryStore[Need])
      def check(url: URL, expectedKey: String) = {
        val key = shortener.shorten(url).value
        key must_== expectedKey
        shortener.expand(key).value must_== Some(url)
      }

      check(url0, "a")
      check(url1, "b")
      check(url2, "c")
    }
  }
}
