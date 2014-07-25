package muster
package codec
package json

import Constants._
import scala.collection.mutable

abstract class JsonOutput[R] extends OutputFormat[R] {

  type Formatter = OutputFormatter[R]

  def indentSpaces: Int

  def createFormatter: Formatter

  def Pretty: JsonOutput[R] = withSpaces(2)

  def into[T](producible: Producible[_, T]): JsonOutput[T] = new ProducibleJsonOutput[T](producible)

  protected def withSpaces(spaces: Int): this.type
}

class ProducibleJsonOutput[T](producible: Producible[_, T], val indentSpaces: Int = 0) extends JsonOutput[T] {
  def createFormatter: Formatter = {
    if (producible == StringProducible) new StringJsonFormatter(producible.toAppendable, indentSpaces).asInstanceOf[JsonFormatter[T]]
    else new UnitJsonFormatter(producible.toAppendable, indentSpaces).asInstanceOf[JsonFormatter[T]]
  }

  protected def withSpaces(spaces: Int): this.type = new ProducibleJsonOutput[T](producible, spaces).asInstanceOf[this.type]
}

trait JsonFormatter[T] extends OutputFormatter[T] {

  protected def writer: muster.Appendable[_]

  protected def spaces: Int

  private[this] val stateStack = mutable.Stack[Int]()

  private[this] def state = stateStack.headOption getOrElse State.None

  private[this] var indentLevel = 0

  private[this] val undefinedEraserBuffer = new mutable.StringBuilder()

  private[this] var appendStrategy: muster.Appendable[_] = writer

  def startArray(name: String = "") {
    writeComma(State.InArray)
    writeStartField(discard = false)
    appendStrategy.append('[')
    stateStack push State.ArrayStarted
  }

  def endArray() {
    appendStrategy.append(']')
    stateStack.pop()
  }

  def startObject(name: String = "") {
    writeComma(State.InArray)
    writeStartField(discard = false)
    appendStrategy.append('{')
    indentLevel += 1
    stateStack push State.ObjectStarted
  }

  private[this] final def indentForPretty() {
    if (spaces > 0) {
      appendStrategy.append('\n')
      appendStrategy.append(" " * spaces * indentLevel)
    }
  }

  def endObject() {
    stateStack.pop()
    indentLevel -= 1
    indentForPretty()
    appendStrategy.append('}')
  }

  def string(value: String) {
    writeComma(State.InArray)
    writeStartField(discard = false)
    appendStrategy.append('"')
    Quoter.jsonQuote(value, writer)
    appendStrategy.append('"')
  }

  def byte(value: Byte) {
    writeComma(State.InArray)
    writeStartField(discard = false)
    appendStrategy.append(value.toString)
  }

  def int(value: Int) {
    writeComma(State.InArray)
    writeStartField(discard = false)
    appendStrategy.append(value.toString)
  }

  def long(value: Long) {
    writeComma(State.InArray)
    writeStartField(discard = false)
    appendStrategy.append(value.toString)
  }

  def bigInt(value: BigInt) {
    writeComma(State.InArray)
    writeStartField(discard = false)
    appendStrategy.append(value.toString)
  }

  def boolean(value: Boolean) {
    writeComma(State.InArray)
    writeStartField(discard = false)
    appendStrategy.append(value.toString)
  }

  def short(value: Short) {
    writeComma(State.InArray)
    writeStartField(discard = false)
    appendStrategy.append(value.toString)
  }

  def float(value: Float) {
    writeComma(State.InArray)
    writeStartField(discard = false)
    appendStrategy.append(value.toString)
  }

  def double(value: Double) {
    writeComma(State.InArray)
    writeStartField(discard = false)
    appendStrategy.append(value.toString)
  }

  def bigDecimal(value: BigDecimal) {
    writeComma(State.InArray)
    writeStartField(discard = false)
    appendStrategy.append(value.toString)
  }

  def writeNull() {
    writeComma(State.InArray)
    writeStartField(discard = false)
    appendStrategy.append("null")
  }

  def undefined() {
    writeStartField(discard = true)
  }

  private[this] def writeStartField(discard: Boolean) {
    appendStrategy = writer
    try {
      if (!discard && undefinedEraserBuffer.nonEmpty) {
        appendStrategy.append(undefinedEraserBuffer.toString())
      }
    } finally {
      undefinedEraserBuffer.clear()
    }
  }


  private[this] final def writeComma(when: Int*) {
    if (state == State.ArrayStarted) {
      stateStack.pop()
      stateStack push State.InArray
    } else if (state == State.ObjectStarted) {
      stateStack.pop()
      stateStack push State.InObject
    } else if (when.contains(state)) {
      appendStrategy.append(',')
    }
  }

  def startField(name: String) {
    appendStrategy = undefinedEraserBuffer
    writeComma(State.InObject)
    indentForPretty()
    appendStrategy.append('"')
    appendStrategy.append(name)
    appendStrategy.append('"')
    appendStrategy.append(':')
  }


  def close() { }

}

private[muster] class StringJsonFormatter(protected val writer: Appendable[_], protected val spaces: Int = 0) extends JsonFormatter[String] {
  def result: String = {
    writer.flush()
    writer.toString
  }
}

private[muster] class UnitJsonFormatter(protected val writer: Appendable[_], protected val spaces: Int = 0) extends JsonFormatter[Unit] {
  def result: Unit = {
    writer.flush()
    ()
  }
}
