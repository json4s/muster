package muster

import java.util.Date
import scala.collection.mutable
import java.text.DateFormat

trait StringOutputFormat extends OutputFormat[String] {
  def writer: java.io.Writer = new java.io.StringWriter()

  def withDateFormat(df: DateFormat): This
}

abstract class DefaultStringFormat extends StringOutputFormat {
  type Formatter = DefaultStringOutputFormatter
  type This = DefaultStringFormat

  def withDateFormat(df: DateFormat): This = new DefaultStringFormat {
    override val dateFormat: DateFormat = df
  }

  def createFormatter: Formatter = new DefaultStringOutputFormatter(writer, dateFormat)

  def freezeFormatter(fmt: Formatter): This = new DefaultStringFormat {
    override val createFormatter: Formatter = fmt
  }
}


object StringOutputFormatter {

  object State {
    val None = 0
    val ArrayStarted = 1
    val InArray = 2
    val ObjectStarted = 3
    val InObject = 4
  }

  val HexAlphabet = "0123456789ABCDEF"
}

class DefaultStringOutputFormatter(writer: java.io.Writer, dateFormat: DateFormat) extends BaseStringOutputFormatter(writer, dateFormat) {
  def withDateFormat(df: DateFormat): this.type = new DefaultStringOutputFormatter(writer, df).asInstanceOf[this.type]

  def withWriter(wrtr: java.io.Writer): this.type = {
    try {
      wrtr.close()
    } catch {
      case _: Throwable =>
    }
    new DefaultStringOutputFormatter(wrtr, dateFormat).asInstanceOf[this.type]
  }
}

abstract class BaseStringOutputFormatter[T <: OutputFormat[String]](val writer: java.io.Writer, dateFormat: DateFormat, quoteStringWith: String = "\"", escapeSpecialChars: Boolean = true) extends OutputFormatter[String] {

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
    if (escapeSpecialChars) JsonOutput.quote(value, writer) else writer.write(value)
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

  def withWriter(wrtr: java.io.Writer): this.type

  def close() {
    writer.close()
  }

}
