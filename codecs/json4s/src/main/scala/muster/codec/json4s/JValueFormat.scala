package muster
package codec
package json4s

import org.json4s.JsonAST.JValue

object JValueFormat extends JValueOutput with InputFormat[Consumable[JValue], Json4sInputCursor[_]] {
  def createCursor(in: Consumable[JValue], mode: Mode): Json4sInputCursor[_] = {
    new EntryJson4sInputCursor(in.value)
  }
}