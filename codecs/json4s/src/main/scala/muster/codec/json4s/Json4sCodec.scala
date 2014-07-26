package muster
package codec
package json4s

import org.json4s.JsonAST.JValue

object Json4sCodec extends JValueOutput with InputFormat[Consumable[JValue], Json4sInputCursor[JValue]] {
  def createCursor(in: Consumable[JValue]): Json4sInputCursor[JValue] = {
    new EntryJson4sInputCursor(in.value)
  }
}