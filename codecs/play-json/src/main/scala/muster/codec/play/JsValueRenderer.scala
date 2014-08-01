package muster
package codec
package play

import output._
import util.State
import _root_.play.api.libs.json._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object JsValueRenderer {
  private final class JsValueOutputFormatter extends OutputFormatter[JsValue] {

    private[this] val stateStack = mutable.Stack[Int]()
    private[this] def state = stateStack.headOption getOrElse State.None
    private[this] val arrStack: mutable.Stack[mutable.ArrayBuffer[JsValue]] =
      mutable.Stack[ArrayBuffer[JsValue]]()
    private[this] val objStack: mutable.Stack[mutable.ArrayBuffer[(String, JsValue)]] =
      mutable.Stack[mutable.ArrayBuffer[(String, JsValue)]]()
    private[this] val fieldNameStack: mutable.Stack[String] = mutable.Stack[String]()

    private[this] var _res: Option[JsValue] = None

    def startArray(name: String = ""): Unit = {
      stateStack push State.ArrayStarted
      arrStack push ArrayBuffer.empty[JsValue]
    }

    def endArray(): Unit = {
      stateStack.pop()
      val arr = arrStack.pop()
      writeValue(JsArray(arr))
      arr.clear()
    }

    def startObject(name: String = ""): Unit = {
      stateStack push State.ObjectStarted
      objStack push ArrayBuffer.empty[(String, JsValue)]
    }

    def endObject(): Unit = {
      stateStack.pop()
      val obj = objStack.pop()
      writeValue(JsObject(obj))
      obj.clear()
    }

    def string(value: String): Unit = writeValue(JsString(value))

    def byte(value: Byte): Unit = writeValue(JsNumber(value))

    def int(value: Int): Unit = writeValue(JsNumber(value))

    def long(value: Long): Unit = writeValue(JsNumber(value))

    def bigInt(value: BigInt): Unit = writeValue(JsNumber(BigDecimal(value)))

    def boolean(value: Boolean): Unit = writeValue(JsBoolean(value))

    def short(value: Short): Unit = writeValue(JsNumber(value))

    def float(value: Float): Unit = writeValue(JsNumber(value))

    def double(value: Double): Unit = writeValue(JsNumber(value))

    def bigDecimal(value: BigDecimal): Unit = writeValue(JsNumber(value))

    def startField(name: String): Unit = {
      if (state == State.ObjectStarted) {
        fieldNameStack push name
      }
    }

    private[this] def writeValue(value: JsValue) {
      if(state == State.ObjectStarted) {
        objStack.head += fieldNameStack.pop() -> value
      } else if (state == State.ArrayStarted) {
        arrStack.head += value
      } else {
        _res = Some(value)
      }

    }

    def writeNull(): Unit = writeValue(JsNull)

    def undefined(): Unit = writeValue(JsUndefined(""))

    def result: JsValue =
      _res getOrElse (throw new IllegalStateException(s"Can't turn ${_res} into an org.json4s.JsonAST.JsValue"))

    def close() {
      arrStack.clear()
      objStack.clear()
      stateStack.clear()
      fieldNameStack.clear()
      _res = None
    }
  }

}
class JsValueRenderer extends Renderer[JsValue] {
  import muster.codec.play.JsValueRenderer._
  type Formatter = OutputFormatter[JsValue]

  def createFormatter: Formatter = new JsValueOutputFormatter().asInstanceOf[OutputFormatter[JsValue]]
}