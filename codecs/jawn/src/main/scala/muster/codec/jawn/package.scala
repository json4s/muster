package muster
package codec

package object jawn {

  object JsonFormat extends ProducibleJsonOutput(StringProducible) with InputFormat[Consumable[_], JawnInputCursor] {
    def createCursor(in: Consumable[_], mode: Mode): JawnInputCursor = new JawnInputCursor(in, mode)
  }
}
