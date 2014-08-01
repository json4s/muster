package muster
package codec
package play

import input.InputFormat
import _root_.play.api.libs.json.JsValue

object PlayJsonCodec extends JsValueRenderer with InputFormat[Consumable[JsValue], Json4sInputCursor[JsValue]] {
  def createCursor(in: Consumable[JsValue]): Json4sInputCursor[JsValue] =
    new { val source: JsValue  = in.value} with Json4sInputCursor[JsValue] {
      override def hasNextNode: Boolean = false
      protected def node = source
  }
}