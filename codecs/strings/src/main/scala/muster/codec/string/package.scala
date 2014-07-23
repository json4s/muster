package muster
package codec

package object string {

  object StringFormatter extends DefaultStringFormat

  implicit class StringProducingObject[T](p: T)(implicit prod: Producer[T])  {
    def asString: String = StringFormatter.from(p)
  }
}

