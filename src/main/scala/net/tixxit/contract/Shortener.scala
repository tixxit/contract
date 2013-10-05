package net.tixxit.contract

import java.net.URL

trait Shortener[M[+_]] {
  def shorten(url: URL): M[String]
  def expand(key: String): M[Option[URL]]
}
