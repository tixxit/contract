package net.tixxit.contract
package web

import spray.httpx.marshalling._
import spray.http._
import spray.http.MediaTypes._

import argonaut._
import Argonaut._

trait ArgonautMarshallers {
  implicit val argonautJsonMarshaller = Marshaller.of[Json](`application/json`) { (json, _, ctx) =>
    ctx.marshalTo(HttpEntity(`application/json`, json.nospaces))
  }
}

object ArgonautMarshallers extends ArgonautMarshallers
