package muster
package codec
package jawn

import muster.codec.json.ProducibleJsonOutput

object JawnCodec extends ProducibleJsonOutput(StringProducible) with InputFormat[Consumable[_], JawnInputCursor] {
  def createCursor(in: Consumable[_]): JawnInputCursor = new JawnInputCursor(in)
}