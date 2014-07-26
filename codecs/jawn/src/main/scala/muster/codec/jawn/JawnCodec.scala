package muster
package codec
package jawn

import muster.codec.json.JsonRenderer

object JawnCodec extends JsonRenderer(StringProducible) with InputFormat[Consumable[_], JawnInputCursor] {
  def createCursor(in: Consumable[_]): JawnInputCursor = new JawnInputCursor(in)
}