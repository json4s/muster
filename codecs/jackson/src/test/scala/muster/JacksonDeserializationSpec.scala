package muster


import muster.codec.jackson._
import muster.codec.json.JsonDeserializationSpec

class JacksonDeserializationSpec extends JsonDeserializationSpec[String](JsonFormat) {
  def parse(value: String): String = value
}

