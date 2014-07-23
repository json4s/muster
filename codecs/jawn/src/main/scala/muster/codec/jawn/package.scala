package muster
package codec

import muster.codec.json.ProducibleJsonOutput

package object jawn {

  object JsonFormat extends ProducibleJsonOutput(StringProducible) with InputFormat[Consumable[_], JawnInputCursor] {
    def createCursor(in: Consumable[_], mode: Mode): JawnInputCursor = new JawnInputCursor(in, mode)
  }
}
