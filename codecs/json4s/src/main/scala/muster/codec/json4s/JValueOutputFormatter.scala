package muster
package codec
package json4s

import muster.Constants.State
import org.json4s.JsonAST._

import scala.collection.mutable

private[json4s] case object JValueProducible extends Producible[JValue, JValue] {
  def value: JValue = ???
  def toAppendable: Appendable[_] = ???
}

object JValueOutput {
  private final class JValueOutputFormatter extends OutputFormatter[JValue] {

    private[this] var state: Int = State.None
    private[this] var arr: mutable.ArrayBuffer[JValue] = mutable.ArrayBuffer.empty[JValue]
    private[this] var obj: mutable.ArrayBuffer[JField] = mutable.ArrayBuffer.empty[JField]
    private[this] var _res: Option[JValue] = None
    private[this] var fieldName: String = null

    def startArray(name: String = ""): Unit = {
      state = State.ArrayStarted
    }

    def endArray(): Unit = {
      state = State.None
      writeValue(JArray(arr.toList))
      arr.clear()
    }

    def startObject(name: String = ""): Unit = state = State.ObjectStarted

    def endObject(): Unit = {
      state = State.None
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
        state = State.InObject
        fieldName = name
      }
    }

    private[this] def writeValue(value: JValue) {
      if(state == State.InObject) {
        obj += fieldName -> value
        fieldName = null
      } else if (state == State.InArray) {
        arr += value
      } else if (state == State.None) {
        _res = Some(value)
      }
    }

    def writeNull(): Unit = writeValue(JNull)

    def undefined(): Unit = writeValue(JNothing)

    def result: JValue =
      _res getOrElse (throw new IllegalStateException(s"Can't turn ${_res} into an org.json4s.JsonAST.JValue"))

    def close() {
      arr.clear()
      obj.clear()
      _res = None
      state = State.None
    }
  }

}
class JValueOutput extends OutputFormat[JValue] {
  import JValueOutput._
  type Formatter = OutputFormatter[JValue]

  def createFormatter: Formatter = new JValueOutputFormatter().asInstanceOf[OutputFormatter[JValue]]
}

