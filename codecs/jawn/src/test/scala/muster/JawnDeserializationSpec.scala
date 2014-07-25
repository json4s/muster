package muster


import muster.codec.jawn.JsonFormat
import muster.codec.json.JsonDeserializationSpec

class JawnDeserializationSpec extends JsonDeserializationSpec[String](JsonFormat) {
  def parse(value: String): String = value
}