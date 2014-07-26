package muster
package codec
package jackson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.MissingNode
import muster.codec.json.JsonRenderer

import scala.util.Try

object JacksonCodec extends JsonRenderer(StringProducible) with JacksonInputFormat[Consumable[_]] {
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
    case ByteChannelConsumable(src) => jic(src) { ch =>
      mapper.readTree(java.nio.channels.Channels.newInputStream(ch))
    }
    case ByteBufferConsumable(src) => jic(src) { ch =>
      mapper.readTree(Consumable.byteBufferInputStream(ch))
    }
  }
}