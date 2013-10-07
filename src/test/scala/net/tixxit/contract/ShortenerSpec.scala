package net.tixxit.contract

import net.tixxit.contract.util._
import net.tixxit.contract.hash._
import net.tixxit.contract.store._

import java.net.URL

import org.specs2.mutable.Specification

import scalaz._
import scalaz.syntax.comonad._

trait ShortenerSpec[M[+_]] extends Specification {
  val url0 = new URL("http://url0.com/")
  val url1 = new URL("http://url1.com/")
  val url2 = new URL("http://url2.com/")

  implicit def coM: Comonad[M]

  def withShortener[A](f: Shortener[M] => A): A

  "ShortenerSpec" should {
    "shortened URL can be expanded" in {
      withShortener { shortener =>
        val key = shortener.shorten(url0).copoint
        shortener.expand(key).copoint must_== Some(url0)
      }
    }

    "unshortened URL cannot be expanded" in {
      withShortener { shortener =>
        shortener.expand("asdf").copoint must_== None
      }
    }

    "multiple URLs can be shortened" in {
      withShortener { shortener =>
        val key0 = shortener.shorten(url0).copoint
        val key1 = shortener.shorten(url1).copoint
        val key2 = shortener.shorten(url2).copoint
        shortener.expand(key1).copoint must_== Some(url1)
        shortener.expand(key0).copoint must_== Some(url0)
        shortener.expand(key2).copoint must_== Some(url2)
      }
    }
  }
}
