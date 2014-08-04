package muster
package codec
package json4s


import muster.codec.json.ObjectDecompositionSpecBase
import org.json4s._

class ObjectJValueDecompositionSpec extends ObjectDecompositionSpecBase[JValue](Json4sCodec) {
  def parse(s: String): JValue = jackson.parseJson(s)
  def astString(s: String): JValue = JString(s)
}