package muster
package codec

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.MissingNode
import muster.codec.json.ProducibleJsonOutput

import scala.util.Try



package object jackson {

  implicit class ProducingObject[T](p: T)(implicit prod: Producer[T])  {
    def asJson = JsonFormat.from(p)
    def asPrettyJson = JsonFormat.Pretty.from(p)
  }
}
