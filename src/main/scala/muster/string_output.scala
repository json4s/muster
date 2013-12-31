package muster

import java.util.Date
import scala.collection.mutable
import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormatter}



trait StringOutputFormat extends OutputFormat[String] {
  def writer: java.io.Writer = new java.io.StringWriter()
  def withDateFormat(df: DateTimeFormatter): This
}

abstract class DefaultStringFormat extends StringOutputFormat {
  type Formatter = DefaultStringOutputFormatter
  type This = DefaultStringFormat

  def withDateFormat(df: DateTimeFormatter): This = new DefaultStringFormat { override val dateFormat: DateTimeFormatter = df }
  def createFormatter: Formatter = new DefaultStringOutputFormatter(writer, dateFormat)
  def freezeFormatter(fmt: Formatter): This = new DefaultStringFormat { override val createFormatter: Formatter = fmt }
}


object StringOutputFormatter {

  object State {
    val None = 0
    val ArrayStarted = 1
    val InArray = 2
    val ObjectStarted = 3
    val InObject = 4
  }

}

class DefaultStringOutputFormatter(writer: java.io.Writer, dateFormat: DateTimeFormatter) extends BaseStringOutputFormatter(writer, dateFormat) {
  def withDateFormat(df: DateTimeFormatter): this.type = new DefaultStringOutputFormatter(writer, df).asInstanceOf[this.type]
  def withWriter(wrtr: java.io.Writer): this.type = {
    try { wrtr.close() } catch { case _: Throwable => }
    new DefaultStringOutputFormatter(wrtr, dateFormat).asInstanceOf[this.type]
  }
}

abstract class BaseStringOutputFormatter[T <: OutputFormat[String]](val writer: java.io.Writer, dateFormat: DateTimeFormatter, quoteStringWith: String = "\"", escapeSpecialChars: Boolean = true) extends OutputFormatter[String] {

  import StringOutputFormatter._

  protected val stateStack = mutable.Stack[Int]()
  protected def state = stateStack.headOption getOrElse State.None

  def startArray(name: String) {
    writeComma(State.InArray)
    writer.write(name)
    writer.write('(')
    stateStack push State.ArrayStarted
  }

  def endArray() {
    writer.write(')')
    stateStack.pop()
  }

  def startObject(name: String) {
    writeComma(State.InArray)
    writer.write(name)
    writer.write('(')
    stateStack push State.ObjectStarted
  }

  def endObject() {
    writer.write(')')
    stateStack.pop()
  }

  def string(value: String) {
    writeComma(State.InArray)
    if (quoteStringWith != null && quoteStringWith.trim.nonEmpty) writer.write(quoteStringWith)
    if (escapeSpecialChars) quote(value) else writer.write(value)
    if (quoteStringWith != null && quoteStringWith.trim.nonEmpty) writer.write(quoteStringWith)
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
    } else if (when contains state) {
      writer.write(',')
      writer.write(' ')
    }
  }

  def startField(name: String) {
    writeComma(State.InObject, State.InArray)
    writer.write(name.trim)
    writer.write(':')
    writer.write(' ')
  }

  def result: String = writer.toString

  private[this] def quote(s: String) {
    var i = 0
    val l = s.length
    while (i < l) {
      val c = s(i)
      if (c == '"') writer.write("\\\"")
      else if (c == '\\') writer.write("\\\\")
      else if (c == '\b') writer.write("\\b")
      else if (c == '\f') writer.write("\\f")
      else if (c == '\n') writer.write("\\n")
      else if (c == '\r') writer.write("\\r")
      else if (c == '\t') writer.write("\\t")
      else if ((c >= '\u0000' && c <= '\u001f') || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100'))
        writer.write("\\u%04x".format(c: Int))
      else writer.write(c)
      i += 1
    }
  }

  def withWriter(wrtr: java.io.Writer): this.type
  def close() { writer.close() }

}
