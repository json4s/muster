package muster
package codec
package argonaut

import _root_.argonaut._
import muster.codec.json.ObjectDecompositionSpecBase

class JsonObjectDecompositionSpec extends ObjectDecompositionSpecBase[Json](ArgonautCodec) {
  def parse(s: String): Json = Parse.parseOption(s).getOrElse(throw new ParseException(s"Couldn't parse value $s", None))
  def astString(s: String): Json = Json.jString(s)
}