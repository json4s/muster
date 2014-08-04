package muster
package codec
package argonaut

import muster.input.InputFormat
import _root_.argonaut._

object ArgonautCodec extends JsonRenderer with InputFormat[Consumable[Json], ArgonautInputCursor[Json]] {
  def createCursor(in: Consumable[Json]): ArgonautInputCursor[Json] =
    new { val source: Json  = in.value} with ArgonautInputCursor[Json] {
      override def hasNextNode: Boolean = false
      protected def node = source
  }
}