package muster
package codec
package argonaut

import _root_.argonaut._
import muster.codec.json.JsonDeserializationSpec
import muster.input.{InputCursor, InputFormat}

class JsonExtractionSpec extends JsonDeserializationSpec[Json](ArgonautCodec.asInstanceOf[InputFormat[Consumable[_], InputCursor[_]]]) {
  def parse(value: String): Json = Parse.parse(value).getOrElse(throw new ParseException(s"Unable to parse json\n$value", None))
}