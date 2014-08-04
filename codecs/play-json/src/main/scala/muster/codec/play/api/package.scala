package muster
package codec
package play

import _root_.play.api.libs.json._

import scala.util.Try

package object api {

  implicit class JsValueProducingObject[T: Producer](p: T) {
    def asJsValue = PlayJsonCodec.from(p)
  }

  implicit class JsValueConsumingObject(jv: JsValue) {
    @inline def as[T](implicit consumer: Consumer[T]) = PlayJsonCodec.as[T](jv)
    @inline def tryAs[T](implicit consumer: Consumer[T]) = Try(as[T])
    @inline def getAs[T](implicit consumer: Consumer[T]) = jv match {
      case JsNull | _: JsUndefined => None
      case _ => tryAs[T].toOption
    }
  }

}
