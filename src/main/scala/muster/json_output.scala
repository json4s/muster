package muster

import scala.collection.mutable
import java.util.Date
import java.text.DateFormat

object JsonOutput {

  object Appendable {
    implicit class StringBuilderAppendable(sb: mutable.StringBuilder) extends  Appendable[StringBuilder] {
      def append(s: String): mutable.StringBuilder = sb.append(s)
    }
    implicit class WriterAppendable(sb: java.io.Writer) extends  Appendable[java.io.Writer] {
      def append(s: String): java.io.Writer = sb.append(s)
    }
  }
  trait Appendable[T] {
    def append(s: String): T
  }

  def quote(s: String, writer: Appendable[_]) {
    var i = 0
    val l = s.length
    while (i < l) {
      val c = s(i)
      if (c == '"') writer.append("\\\"")
      else if (c == '\\') writer.append("\\\\")
      else if (c == '\b') writer.append("\\b")
      else if (c == '\f') writer.append("\\f")
      else if (c == '\n') writer.append("\\n")
      else if (c == '\r') writer.append("\\r")
      else if (c == '\t') writer.append("\\t")
      else if ((c >= '\u0000' && c <= '\u001f') || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100')) {
        writer.append("\\u%04x".format(c: Int))
      } else writer.append(c.toString)
      i += 1
    }
  }

//  private class PrettyJsonOutput[R](val indentSpaces: Int = 2, ) extends JsonOutput[R]
}

abstract class JsonOutput[R] extends OutputFormat[R] {

  type Formatter = JsonFormatter[R]

  def indentSpaces: Int

  def createFormatter: Formatter

  def Pretty: JsonOutput[R] = withSpaces(2)

  def into[T](producible: Producible[_, T]): JsonOutput[T] = new ProducibleJsonOutput[T](producible)

  protected def withSpaces(spaces: Int): this.type
}

class ProducibleJsonOutput[T](producible: Producible[_, T], val indentSpaces: Int = 0) extends JsonOutput[T] {
  def createFormatter: Formatter = {
    if (producible == StringProducible) new StringJsonFormatter(producible.toWriter, indentSpaces).asInstanceOf[JsonFormatter[T]]
    else new UnitJsonFormatter(producible.toWriter, indentSpaces).asInstanceOf[JsonFormatter[T]]
  }

  protected def withSpaces(spaces: Int): this.type = new ProducibleJsonOutput[T](producible, spaces).asInstanceOf[this.type]
}

trait JsonFormatter[T] extends OutputFormatter[T] {


  protected def writer: java.io.Writer

  protected def spaces: Int

  import StringOutputFormatter._

  private[this] val stateStack = mutable.Stack[Int]()

  private[this] def state = stateStack.headOption getOrElse State.None

  private[this] var indentLevel = 0

  def startArray(name: String = "") {
    writeComma(State.InArray)
    writer.write('[')
    stateStack push State.ArrayStarted
  }

  def endArray() {
    writer.write(']')
    stateStack.pop()
  }

  def startObject(name: String = "") {
    writeComma(State.InArray)
    writer.write('{')
    indentLevel += 1
    stateStack push State.ObjectStarted
  }

  private[this] final def indentForPretty() {
    if (spaces > 0) {
      writer.write('\n')
      writer.write(" " * spaces * indentLevel)
    }
  }

  def endObject() {
    stateStack.pop()
    indentLevel -= 1
    indentForPretty()
    writer.write('}')
  }

  def string(value: String) {
    writeComma(State.InArray)
    writer.write('"')
    JsonOutput.quote(value, writer)
    writer.write('"')
  }

  def byte(value: Byte) {
    writeComma(State.InArray)
    writer.write(value.toString)
  }

  def int(value: Int) {
    writeComma(State.InArray)
    writer.write(value.toString)
  }

  def long(value: Long) {
    writeComma(State.InArray)
    writer.write(value.toString)
  }

  def bigInt(value: BigInt) {
    writeComma(State.InArray)
    writer.write(value.toString)
  }

  def boolean(value: Boolean) {
    writeComma(State.InArray)
    writer.write(value.toString)
  }

  def short(value: Short) {
    writeComma(State.InArray)
    writer.write(value.toString)
  }

  def float(value: Float) {
    writeComma(State.InArray)
    writer.write(value.toString)
  }

  def double(value: Double) {
    writeComma(State.InArray)
    writer.write(value.toString)
  }

  def bigDecimal(value: BigDecimal) {
    writeComma(State.InArray)
    writer.write(value.toString)
  }

  def writeNull() {
    writeComma(State.InArray)
    writer.write("null")
  }

  def undefined() {}

  private[this] final def writeComma(when: Int*) {
    if (state == State.ArrayStarted) {
      stateStack.pop()
      stateStack push State.InArray
    } else if (state == State.ObjectStarted) {
      stateStack.pop()
      stateStack push State.InObject
    } else if (when.contains(state)) {
      writer.write(',')
    }
  }

  def startField(name: String) {
    writeComma(State.InObject)
    indentForPretty()
    writer.write('"')
    writer.write(name)
    writer.write('"')
    writer.write(':')
  }


  def close() { }

}

private[muster] class StringJsonFormatter(protected val writer: java.io.Writer, protected val spaces: Int = 0) extends JsonFormatter[String] {
  def result: String = {
    writer.flush()
    writer.toString
  }
}

private[muster] class UnitJsonFormatter(protected val writer: java.io.Writer, protected val spaces: Int = 0) extends JsonFormatter[Unit] {
  def result: Unit = {
    writer.flush()
    ()
  }
}
