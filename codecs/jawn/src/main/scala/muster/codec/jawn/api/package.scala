package muster
package codec
package jawn

package object api {

  implicit class ProducingObject[T](p: T)(implicit prod: Producer[T])  {
      def asJson = JawnCodec.from(p)
      def asPrettyJson = JawnCodec.Pretty.from(p)
    }
}
