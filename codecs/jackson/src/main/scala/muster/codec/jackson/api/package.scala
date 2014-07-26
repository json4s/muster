package muster
package codec
package jackson

package object api {

  implicit class ProducingObject[T](p: T)(implicit prod: Producer[T])  {
    def asJson = JacksonCodec.from(p)
    def asPrettyJson = JacksonCodec.Pretty.from(p)
  }
}
