package net.tixxit.contract
package web

import java.net.URLEncoder

import scala.concurrent.Future

import org.specs2.mutable.Specification

import spray.testkit.Specs2RouteTest
import spray.http._
import spray.http.StatusCodes._
import spray.http.MediaTypes._

import argonaut._
import Argonaut._

import scalaz.contrib.std.scalaFuture._
import ArgonautMarshallers._

class ShortenerServiceSpec extends Specification with Specs2RouteTest with ShortenerService {
  implicit def executionContext = system.dispatcher
  def actorRefFactory = system

  val shortener = HashShortener(hash.MD5HashStream, new store.InMemoryStore[Future])

  def fillForm(url: String): HttpEntity = {
    val encoded = URLEncoder.encode(url, "UTF-8")
    HttpEntity(`application/x-www-form-urlencoded`, s"url=$encoded")
  }

  val google = "http://google.com/"
  val website = "http://tomswitzer.net/"

  "ShortenerService" should {
    "GET root path returns index path" in {
      Get() ~> uiRoute ~> check {
        entityAs[String] must contain("Contract")
        entityAs[String] must contain("<form")
      }
    }

    "GET to /static/ passes through to resources/web/static directory" in {
      Get("/static/css/contract.css") ~> uiRoute ~> check {
        entityAs[String] must contain("#contract")
      }
    }

    "GET of non-existent contracted URL returns 404" in {
      Get("/asdfasdf") ~> shortenRoute ~> check {
        status === NotFound
      }
    }

    "POST to root redirects to metadata of contracted URL" in {
      Post("/", fillForm(google)) ~> shortenRoute ~> check {
        status === Found
        header("Location") map (_.value) should beLike { case Some(path) =>
          Get(path) ~> shortenRoute ~> check {
            status === OK
            val result = entityAs[Json].jdecode[Map[String,String]].toOption
            result.flatMap { obj => obj.get("url") } === Some(google)
          }
        }
      }
    }

    "POST without URL is rejected" in {
      Post("/") ~> shortenRoute ~> check {
        handled must beFalse
      }
    }

    "GET of shortened URL redirects to URL" in {
      Post("/", fillForm(website)) ~> shortenRoute ~> check {
        header("Location") map (_.value) should beLike { case Some(path) =>
          Get(path) ~> shortenRoute ~> check {
            val result = entityAs[Json].jdecode[Map[String,String]].toOption
            result.flatMap { obj => obj.get("key") } should beLike { case Some(key) =>
              Get(s"/$key") ~> shortenRoute ~> check {
                status === MovedPermanently
                val url: Option[String] = header("Location") map (_.value)
                url === Some(website)
              }
            }
          }
        }
      }
    }

    "URL cannot be FTP or other random protocols" in {
      Post("/", fillForm("ftp://asdf.com/")) ~> shortenRoute ~> check {
        status === BadRequest
      }

      Post("/", fillForm("gopher://asdf.com/")) ~> shortenRoute ~> check {
        status === BadRequest
      }

      Post("/", fillForm("steam://asdf.com/")) ~> shortenRoute ~> check {
        status === BadRequest
      }
    }

    "URL can be HTTPS" in {
      Post("/", fillForm("https://asdf.com/")) ~> shortenRoute ~> check {
        status === Found
      }
    }
  }
}
