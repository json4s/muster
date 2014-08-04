package muster
package codec
package argonaut

import _root_.argonaut._

import scala.util.Try

package object api {
  implicit class JsonProducingObject[T: Producer](p: T) {
      def asJson = ArgonautCodec.from(p)
    }

    implicit class JsValueConsumingObject(jv: Json) {
      @inline def as[T](implicit consumer: Consumer[T]) = ArgonautCodec.as[T](jv)
      @inline def tryAs[T](implicit consumer: Consumer[T]) = Try(as[T])
      @inline def getAs[T](implicit consumer: Consumer[T]) = if(jv.isNull) None else tryAs[T].toOption
    }
}
