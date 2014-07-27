package muster
package codec
package json4s

import muster.input.InputFormat
import org.json4s.JsonAST.JValue

object Json4sCodec extends JValueRenderer with InputFormat[Consumable[JValue], Json4sInputCursor[JValue]] {
  def createCursor(in: Consumable[JValue]): Json4sInputCursor[JValue] =
    new { val source: JValue  = in.value} with Json4sInputCursor[JValue] {
      override def hasNextNode: Boolean = false
      protected def node = source
  }
}