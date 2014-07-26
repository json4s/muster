package muster
package codec
package json4s

import muster.Constants.State
import org.json4s.JsonAST._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

private[json4s] case object JValueProducible extends Producible[JValue, JValue] {
  def value: JValue = ???
  def toAppendable: Appendable[_] = ???
}

object JValueOutput {
  private final class JValueRenderer extends Renderer[JValue] {

    private[this] val stateStack = mutable.Stack[Int]()
    private[this] def state = stateStack.headOption getOrElse State.None
    private[this] val arrStack: mutable.Stack[mutable.ArrayBuffer[JValue]] =
      mutable.Stack[ArrayBuffer[JValue]]()
    private[this] val objStack: mutable.Stack[mutable.ArrayBuffer[JField]] =
      mutable.Stack[mutable.ArrayBuffer[JField]]()
    private[this] val fieldNameStack: mutable.Stack[String] = mutable.Stack[String]()

    private[this] var _res: Option[JValue] = None

    def startArray(name: String = ""): Unit = {
      stateStack push State.ArrayStarted
      arrStack push ArrayBuffer.empty[JValue]
    }

    def endArray(): Unit = {
      stateStack.pop()
      val arr = arrStack.pop()
      writeValue(JArray(arr.toList))
      arr.clear()
    }

    def startObject(name: String = ""): Unit = {
      stateStack push State.ObjectStarted
      objStack push ArrayBuffer.empty[JField]
    }

    def endObject(): Unit = {
      stateStack.pop()
      val obj = objStack.pop()
      writeValue(JObject(obj.toList))
      obj.clear()
    }

    def string(value: String): Unit = writeValue(JString(value))

    def byte(value: Byte): Unit = writeValue(JInt(value))

    def int(value: Int): Unit = writeValue(JInt(value))

    def long(value: Long): Unit = writeValue(JInt(value))

    def bigInt(value: BigInt): Unit = writeValue(JInt(value))

    def boolean(value: Boolean): Unit = writeValue(JBool(value))

    def short(value: Short): Unit = writeValue(JInt(value))

    def float(value: Float): Unit = writeValue(JDouble(value))

    def double(value: Double): Unit = writeValue(JDouble(value))

    def bigDecimal(value: BigDecimal): Unit = writeValue(JDecimal(value))

    def startField(name: String): Unit = {
      if (state == State.ObjectStarted) {
        fieldNameStack push name
      }
    }

    private[this] def writeValue(value: JValue) {
      if(state == State.ObjectStarted) {
        objStack.head += fieldNameStack.pop() -> value
      } else if (state == State.ArrayStarted) {
        arrStack.head += value
      } else {
        _res = Some(value)
      }

    }

    def writeNull(): Unit = writeValue(JNull)

    def undefined(): Unit = writeValue(JNothing)

    def result: JValue =
      _res getOrElse (throw new IllegalStateException(s"Can't turn ${_res} into an org.json4s.JsonAST.JValue"))

    def close() {
      arrStack.clear()
      objStack.clear()
      stateStack.clear()
      fieldNameStack.clear()
      _res = None
    }
  }

}
class JValueOutput extends OutputFormat[JValue] {
  import muster.codec.json4s.JValueOutput._
  type Formatter = Renderer[JValue]

  def createFormatter: Formatter = new JValueRenderer().asInstanceOf[Renderer[JValue]]
}

