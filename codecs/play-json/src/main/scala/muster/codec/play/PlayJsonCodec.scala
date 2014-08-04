package muster
package codec
package play

import input.InputFormat
import _root_.play.api.libs.json.JsValue

object PlayJsonCodec extends JsValueRenderer with InputFormat[Consumable[JsValue], PlayJsonInputCursor[JsValue]] {
  def createCursor(in: Consumable[JsValue]): PlayJsonInputCursor[JsValue] =
    new { val source: JsValue  = in.value} with PlayJsonInputCursor[JsValue] {
      override def hasNextNode: Boolean = false
      protected def node = source
  }
}