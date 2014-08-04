package muster
package codec
package play

import muster.codec.json.JsonDeserializationSpec
import muster.input.{InputCursor, InputFormat}
import _root_.play.api.libs.json._

class JsValueExtractionSpec extends JsonDeserializationSpec[JsValue](PlayJsonCodec.asInstanceOf[InputFormat[Consumable[_], InputCursor[_]]]) {
  def parse(value: String): JsValue = Json.parse(value)
}


