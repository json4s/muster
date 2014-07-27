package muster
package codec
package json

import java.io._
import java.nio.ByteBuffer

import muster.util.State
import muster.util.Quoter
import scala.collection.mutable
import scala.language.higherKinds

object JsonRenderer {
  private final class StringJsonFormatter(protected val writer: util.Appendable[String], protected val spaces: Int = 0) extends JsonFormatter[String] {
    def result: String = {
      writer.flush()
      writer.result()
    }
  }
  private final class ByteArrayJsonFormatter(protected val writer: util.Appendable[Array[Byte]], protected val spaces: Int = 0) extends JsonFormatter[Array[Byte]] {
    def result: Array[Byte] = {
      writer.flush()
      writer.result()
    }
  }
  private final class ByteBufferJsonFormatter(protected val writer: util.Appendable[ByteBuffer], protected val spaces: Int = 0) extends JsonFormatter[ByteBuffer] {
    def result: ByteBuffer = {
      writer.flush()
      writer.result()
    }
  }

  private final class UnitJsonFormatter(protected val writer: util.Appendable[Unit], protected val spaces: Int = 0) extends JsonFormatter[Unit] {
    def result: Unit = {
      writer.flush()
      writer.result()
    }
  }
}

class JsonRenderer[T](producible: Producible[_, T], val indentSpaces: Int = 0) extends Renderer[T] {
  import JsonRenderer._
  type Formatter = OutputFormatter[T]

  def createFormatter: Formatter = {
    if (producible == StringProducible)
      new StringJsonFormatter(producible.toAppendable.asInstanceOf[util.Appendable[String]], indentSpaces).asInstanceOf[JsonFormatter[T]]
    else if (producible == ByteArrayProducible)
      new ByteArrayJsonFormatter(producible.toAppendable.asInstanceOf[util.Appendable[Array[Byte]]], indentSpaces).asInstanceOf[JsonFormatter[T]]
    else if (producible == ByteBufferProducible)
      new ByteBufferJsonFormatter(producible.toAppendable.asInstanceOf[util.Appendable[ByteBuffer]], indentSpaces).asInstanceOf[JsonFormatter[T]]
    else
      new UnitJsonFormatter(producible.toAppendable.asInstanceOf[util.Appendable[Unit]], indentSpaces).asInstanceOf[JsonFormatter[T]]
  }

  protected def withSpaces(spaces: Int): this.type = new JsonRenderer[T](producible, spaces).asInstanceOf[this.type]
  
  def Pretty: JsonRenderer[T] = withSpaces(2)
  
  def into[R](producible: Producible[_, R]): JsonRenderer[R] = new JsonRenderer[R](producible)
}

trait JsonFormatter[T] extends OutputFormatter[T] {

  protected def writer: util.Appendable[T]

  protected def spaces: Int

  private[this] val stateStack = mutable.Stack[Int]()

  private[this] def state = stateStack.headOption getOrElse State.None

  private[this] var indentLevel = 0

  private[this] val undefinedEraserBuffer = new StringBuilder()

  private[this] var appendStrategy: util.Appendable[_] = writer

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
    appendStrategy = util.Appendable.forString(undefinedEraserBuffer)
    writeComma(State.InObject)
    indentForPretty()
    appendStrategy.append('"')
    appendStrategy.append(name)
    appendStrategy.append('"')
    appendStrategy.append(':')
  }


  def close() { }

}


