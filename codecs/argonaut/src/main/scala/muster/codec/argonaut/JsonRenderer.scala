package muster
package codec
package argonaut

import muster.output.{Renderer, OutputFormatter}
import muster.util.State

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import _root_.argonaut._, Argonaut._

object JsonRenderer {
  private final class JsonOutputFormatter extends OutputFormatter[Json] {
    private[this] type JField = (JsonField, Json)
    private[this] val stateStack = mutable.Stack[Int]()
    private[this] def state = stateStack.headOption getOrElse State.None
    private[this] val arrStack: mutable.Stack[mutable.ArrayBuffer[Json]] =
      mutable.Stack[ArrayBuffer[Json]]()
    private[this] val objStack: mutable.Stack[mutable.ArrayBuffer[JField]] =
      mutable.Stack[mutable.ArrayBuffer[JField]]()
    private[this] val fieldNameStack: mutable.Stack[String] = mutable.Stack[String]()

    private[this] var _res: Option[Json] = None

    def startArray(name: String = ""): Unit = {
      stateStack push State.ArrayStarted
      arrStack push ArrayBuffer.empty[Json]
    }

    def endArray(): Unit = {
      stateStack.pop()
      val arr = arrStack.pop()
      writeValue(Json.array(arr:_*))
      arr.clear()
    }

    def startObject(name: String = ""): Unit = {
      stateStack push State.ObjectStarted
      objStack push ArrayBuffer.empty[JField]
    }

    def endObject(): Unit = {
      stateStack.pop()
      val obj = objStack.pop()
      writeValue(Json.obj(obj:_*))
      obj.clear()
    }

    def string(value: String): Unit = writeValue(jString(value))

    def byte(value: Byte): Unit = writeValue(jNumber(value))

    def int(value: Int): Unit = writeValue(jNumber(value))

    def long(value: Long): Unit = writeValue(jNumber(value))

    def bigInt(value: BigInt): Unit = writeValue(jNumber(value.toDouble))

    def boolean(value: Boolean): Unit = writeValue(jBool(value))

    def short(value: Short): Unit = writeValue(jNumber(value))

    def float(value: Float): Unit = writeValue(jNumber(value))

    def double(value: Double): Unit = writeValue(jNumber(value))

    def bigDecimal(value: BigDecimal): Unit = writeValue(jNumber(value.toDouble))

    def startField(name: String): Unit = {
      if (state == State.ObjectStarted) {
        fieldNameStack push name
      }
    }

    private[this] def writeValue(value: Json) {
      if(state == State.ObjectStarted) {
        objStack.head += fieldNameStack.pop() -> value
      } else if (state == State.ArrayStarted) {
        arrStack.head += value
      } else {
        _res = Some(value)
      }

    }

    def writeNull(): Unit = writeValue(jNull)

    def undefined(): Unit = {
      if (state == State.ObjectStarted) {
        fieldNameStack.pop()
      }
    }

    def result: Json =
      _res getOrElse (throw new IllegalStateException(s"Can't turn ${_res} into an argonaut.Json value"))

    def close() {
      arrStack.clear()
      objStack.clear()
      stateStack.clear()
      fieldNameStack.clear()
      _res = None
    }
  }

}
class JsonRenderer extends Renderer[Json] {
  import muster.codec.argonaut.JsonRenderer._
  type Formatter = OutputFormatter[Json]

  def createFormatter: Formatter = new JsonOutputFormatter().asInstanceOf[OutputFormatter[Json]]
}
