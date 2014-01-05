package muster

import java.util.Date
import java.text.{SimpleDateFormat, DateFormat}

trait OutputFormatter[R] extends AutoCloseable {
  def startArray(name: String = ""): Unit

  def endArray(): Unit

  def startObject(name: String = ""): Unit

  def endObject(): Unit

  def string(value: String): Unit

  def byte(value: Byte): Unit

  def int(value: Int): Unit

  def long(value: Long): Unit

  def bigInt(value: BigInt): Unit

  def boolean(value: Boolean): Unit

  def short(value: Short): Unit

  def float(value: Float): Unit

  def double(value: Double): Unit

  def bigDecimal(value: BigDecimal): Unit

  def date(value: Date): Unit

  def startField(name: String): Unit

  def writeNull(): Unit

  def undefined(): Unit

  def result: R

  def withDateFormat(df: DateFormat): this.type

  def close()
}


trait OutputFormat[R] {
  type Formatter <: OutputFormatter[R]
  type This <: OutputFormat[R]

  def dateFormat: DateFormat = SafeSimpleDateFormat.Iso8601Formatter

  def withDateFormat(df: DateFormat): This

  def createFormatter: Formatter

  def freezeFormatter(fmt: Formatter): This

  def from[T](out: T)(implicit fmt: Producer[T]): R = fmt.writeFormatted(out, this)
}
