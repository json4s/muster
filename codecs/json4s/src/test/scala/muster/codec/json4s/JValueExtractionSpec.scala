package muster
package codec
package json4s

import muster.codec.json.JsonDeserializationSpec
import org.json4s.JsonAST.JValue

class JValueExtractionSpec extends JsonDeserializationSpec[JValue](Json4sCodec.asInstanceOf[InputFormat[Consumable[_], InputCursor[_]]]) {
  def parse(value: String): JValue = org.json4s.jackson.parseJson(value)
}