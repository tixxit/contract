package net.tixxit.contract
package web

import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
import spray.http._
import spray.http.MediaTypes._

import argonaut._
import Argonaut._

trait ArgonautMarshallers {
  implicit val argonautJsonMarshaller = Marshaller.of[Json](`application/json`) { (json, _, ctx) =>
    ctx.marshalTo(HttpEntity(`application/json`, json.nospaces))
  }

  implicit object ArgonautJsonUnmarshaller extends SimpleUnmarshaller[Json] {
    val canUnmarshalFrom: Seq[ContentTypeRange] = Seq(`application/json`)
    def unmarshal(entity: HttpEntity): Either[DeserializationError, Json] =
      Parse.parse(entity.asString).leftMap(MalformedContent(_)).toEither
  }
}

object ArgonautMarshallers extends ArgonautMarshallers
