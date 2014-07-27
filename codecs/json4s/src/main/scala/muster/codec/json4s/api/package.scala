package muster
package codec
package json4s

import org.json4s.JsonAST._

import scala.util.Try

package object api {
  implicit class JValueProducingObject[T:Producer](p: T) {
    def asJValue = Json4sCodec.from(p)
  }

  implicit class JValueConsumingObject(jv: JValue) {
    @inline def as[T](implicit consumer: Consumer[T]) = Json4sCodec.as[T](jv)
    @inline def tryAs[T](implicit consumer: Consumer[T]) = Try(as[T])
    @inline def getAs[T](implicit consumer: Consumer[T]) = jv match {
      case JNull | JNothing => None
      case _ => tryAs[T].toOption
    }
  }
}
