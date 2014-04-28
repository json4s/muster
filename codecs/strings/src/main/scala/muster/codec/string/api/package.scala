package muster
package codec
package string

package object api {

  object StringFormatter extends DefaultStringFormat
  implicit class StringProducingObject[T](p: T)(implicit prod: Producer[T])  {
    def asString: String = StringFormatter.from(p)
  }
}

