package muster
package codec
package string

import scala.collection.mutable

abstract class StringRenderer extends Renderer[String] {
  type Formatter = StringOutputFormatter
  def createFormatter: Formatter = new StringOutputFormatter(Appendable.forString())
}

class StringOutputFormatter(val writer: Appendable[String], quoteStringWith: String = "\"", escapeSpecialChars: Boolean = true) extends OutputFormatter[String] {

  import Constants._

  protected val stateStack = mutable.Stack[Int]()

  protected def state = stateStack.headOption getOrElse State.None

  def startArray(name: String) {
    writeComma(State.InArray)
    writer.append(name)
    writer.append('(')
    stateStack push State.ArrayStarted
  }

  def endArray() {
    writer.append(')')
    stateStack.pop()
  }

  def startObject(name: String) {
    writeComma(State.InArray)
    writer.append(name)
    writer.append('(')
    stateStack push State.ObjectStarted
  }

  def endObject() {
    writer.append(')')
    stateStack.pop()
  }

  def string(value: String) {
    writeComma(State.InArray)
    if (quoteStringWith != null && quoteStringWith.trim.nonEmpty) writer.append(quoteStringWith)
    if (escapeSpecialChars) Quoter.jsonQuote(value, writer) else writer.append(value)
    if (quoteStringWith != null && quoteStringWith.trim.nonEmpty) writer.append(quoteStringWith)
  }

  def byte(value: Byte) {
    writeComma(State.InArray)
    writer.append(value.toString)
  }

  def int(value: Int) {
    writeComma(State.InArray)
    writer.append(value.toString)
  }

  def long(value: Long) {
    writeComma(State.InArray)
    writer.append(value.toString)
  }

  def bigInt(value: BigInt) {
    writeComma(State.InArray)
    writer.append(value.toString)
  }

  def boolean(value: Boolean) {
    writeComma(State.InArray)
    writer.append(value.toString)
  }

  def short(value: Short) {
    writeComma(State.InArray)
    writer.append(value.toString)
  }

  def float(value: Float) {
    writeComma(State.InArray)
    writer.append(value.toString)
  }

  def double(value: Double) {
    writeComma(State.InArray)
    writer.append(value.toString)
  }

  def bigDecimal(value: BigDecimal) {
    writeComma(State.InArray)
    writer.append(value.toString)
  }

  def writeNull() {
    writeComma(State.InArray)
    writer.append("null")
  }

  def undefined() {}

  private[this] def writeComma(when: Int*) {
    if (state == State.ArrayStarted) {
      stateStack.pop()
      stateStack push State.InArray
    } else if (state == State.ObjectStarted) {
      stateStack.pop()
      stateStack push State.InObject
    } else if (when contains state) {
      writer.append(',')
      writer.append(' ')
    }
  }

  def startField(name: String) {
    writeComma(State.InObject, State.InArray)
    writer.append(name.trim)
    writer.append(':')
    writer.append(' ')
  }

  def result: String = writer.toString

  def close() {
    writer.close()
  }

}
