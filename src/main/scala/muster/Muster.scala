package muster

import com.fasterxml.jackson.databind.JsonNode
import scala.util.Try
import com.fasterxml.jackson.databind.node.MissingNode

object Muster {
  object produce {

    object String extends DefaultStringFormat

    object CompactJsonString extends JsonOutput

  }

  object consume {

    object Json extends JacksonInputFormat[Consumable[_]] {

      private def jic[T](src: T)(fn: (T) => JsonNode): JacksonInputCursor[T] = new JacksonInputCursor[T] {
        protected val node: JsonNode = Try(fn(src)).getOrElse(MissingNode.getInstance())
        val source: T = src
      }

      def createCursor(in: Consumable[_]): JacksonInputCursor[_] = in match {
        case StringConsumable(src) => jic(src)(mapper.readTree)
        case FileConsumable(src) => jic(src)(mapper.readTree)
        case ReaderConsumable(src) => jic(src)(mapper.readTree)
        case InputStreamConsumable(src) => jic(src)(mapper.readTree)
        case ByteArrayConsumable(src) => jic(src)(mapper.readTree)
        case URLConsumable(src) => jic(src)(mapper.readTree)
      }
    }

  }

}