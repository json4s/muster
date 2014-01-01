package muster

import org.joda.time.format.DateTimeFormatter
import scala.collection.mutable
import java.util.Date
import org.joda.time.DateTime

abstract class JsonOutput extends StringOutputFormat {
  type Formatter = CompactJsonStringFormatter
  type This = JsonOutput

  def withDateFormat(df: DateTimeFormatter): This =
    new JsonOutput { override val dateFormat: DateTimeFormatter = df }

  def createFormatter: Formatter = new CompactJsonStringFormatter(writer, dateFormat)

  def freezeFormatter(fmt: Formatter): This =
    new JsonOutput { override val createFormatter: Formatter = fmt }
}


class CompactJsonStringFormatter(writer: java.io.Writer, dateFormat: DateTimeFormatter)  extends OutputFormatter[String] {
  import StringOutputFormatter._

  def withDateFormat(df: DateTimeFormatter): this.type = new CompactJsonStringFormatter(writer, df).asInstanceOf[this.type]
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
    quote(value)
    writer.write('"')
  }

  private[this] def quote(s: String) {
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
        writer.write("\\u")
        writer.write(HexAlphabet.charAt(c >> 12 & 0x000F))
        writer.write(HexAlphabet.charAt(c >>  8 & 0x000F))
        writer.write(HexAlphabet.charAt(c >>  6 & 0x000F))
        writer.write(HexAlphabet.charAt(c >>  0 & 0x000F))
      } else writer.append(c.toString)
      i += 1
    }
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
    dateTime(new DateTime(value))
  }

  def dateTime(value: DateTime) {
    writeComma(State.InArray)
    dateFormat.printTo(writer, value)
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
    try { writer.close() } catch { case _: Throwable => }
  }
}
