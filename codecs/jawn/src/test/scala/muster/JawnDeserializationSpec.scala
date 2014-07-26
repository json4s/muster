package muster


import muster.codec.jawn.JawnCodec
import muster.codec.json.JsonDeserializationSpec

class JawnDeserializationSpec extends JsonDeserializationSpec[String](JawnCodec) {
  def parse(value: String): String = value
}