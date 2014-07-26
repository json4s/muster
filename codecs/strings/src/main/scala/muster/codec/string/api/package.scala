package muster
package codec
package string

package object api {

  implicit class StringProducingObject[T](p: T)(implicit prod: Producer[T])  {
    def asString: String = StringCodec.from(p)
  }
}

