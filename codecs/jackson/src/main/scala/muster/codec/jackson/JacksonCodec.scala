package muster
package codec
package jackson

import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, JsonMappingException, JsonNode}
import com.fasterxml.jackson.databind.node.MissingNode
import muster.codec.json.JsonRenderer
import muster.input.InputFormat
import muster.output.Renderer

import scala.util.Try

/** Provides a codec using the jackson library for dealing with json
  *
  * It combines a [[Renderer]] with a [[muster.input.InputFormat]] to provide
  * bidirectional encoding and decoding of JSON streams
  */
object JacksonCodec extends JsonRenderer(StringProducible) with InputFormat[Consumable[_], JacksonInputCursor[_]] {
  val mapper: ObjectMapper = new ObjectMapper()
  mapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)

  private def jic[T](src: T)(fn: (T) => JsonNode): JacksonInputCursor[T] = new JacksonInputCursor[T] {
    protected val node: JsonNode = Try(fn(src)).getOrElse(MissingNode.getInstance())
    val source: T = src
  }

  def createCursor(in: Consumable[_]): JacksonInputCursor[_] = try {
    in match {
      case StringConsumable(src) => jic(src)(mapper.readTree)
      case FileConsumable(src) => jic(src)(mapper.readTree)
      case ReaderConsumable(src) => jic(src)(mapper.readTree)
      case InputStreamConsumable(src) => jic(src)(mapper.readTree)
      case ByteArrayConsumable(src) => jic(src)(mapper.readTree)
      case URLConsumable(src) => jic(src)(mapper.readTree)
      case ByteChannelConsumable(src) => jic(src) { ch =>
        mapper.readTree(java.nio.channels.Channels.newReader(ch, StandardCharsets.UTF_8.name()))
      }
      case ByteBufferConsumable(src) => jic(src) { ch =>
        mapper.readTree(Consumable.byteBufferInputStream(ch))
      }
    }
  } catch {
    case t: JsonParseException =>
      val loc = Option(t.getLocation) map { l =>
        val source = l.getSourceRef match {
          case null => None
          case f: File => Some(f.getAbsolutePath)
          case u: URL => Some(u.toExternalForm)
          case a => Some(a.toString)
        }
        ParseLocation(l.getLineNr, l.getColumnNr, source)
      }

      throw new ParseException(t.getOriginalMessage, loc)
    case t: JsonMappingException =>
      throw new MappingException(t.getMessage)
  }
}