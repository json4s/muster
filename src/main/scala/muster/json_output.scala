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

}
abstract class JsonOutput extends StringOutputFormat {
  type Formatter = CompactJsonStringFormatter
  type This = JsonOutput

  def withDateFormat(df: DateFormat): This =
    new JsonOutput {
      override val dateFormat: DateFormat = df
    }

  def createFormatter: Formatter = new CompactJsonStringFormatter(writer, dateFormat)

  def freezeFormatter(fmt: Formatter): This =
    new JsonOutput {
      override val createFormatter: Formatter = fmt
    }
}


class CompactJsonStringFormatter(writer: java.io.Writer, dateFormat: DateFormat) extends OutputFormatter[String] {

  import StringOutputFormatter._

  def withDateFormat(df: DateFormat): this.type = new CompactJsonStringFormatter(writer, df).asInstanceOf[this.type]

  def withWriter(wrtr: java.io.Writer): CompactJsonStringFormatter = new CompactJsonStringFormatter(wrtr, dateFormat)

  private[this] val stateStack = mutable.Stack[Int]()

  private[this] def state = stateStack.headOption getOrElse State.None

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
    stateStack push State.ObjectStarted
  }

  def endObject() {
    writer.write('}')
    stateStack.pop()
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
    writeComma()
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

  def date(value: Date) {
    writeComma(State.InArray)
    writer.write(dateFormat.format(value))
  }

  def writeNull() {
    writeComma(State.InArray)
    writer.write("null")
  }

  def undefined() {}

  private[this] def writeComma(when: Int*) {
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
    writer.write('"')
    writer.write(name)
    writer.write('"')
    writer.write(':')
  }

  def result: String = writer.toString

  def close() {
    try {
      writer.close()
    } catch {
      case _: Throwable =>
    }
  }
}
