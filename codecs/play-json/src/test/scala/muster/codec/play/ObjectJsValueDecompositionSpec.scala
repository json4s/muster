package muster
package codec
package play

import _root_.play.api.libs.json._
import muster.codec.json.ObjectDecompositionSpecBase

class ObjectJsValueDecompositionSpec extends ObjectDecompositionSpecBase[JsValue](PlayJsonCodec) {
  def parse(s: String): JsValue = Json.parse(s)
  def astString(s: String): JsValue = JsString(s)
}