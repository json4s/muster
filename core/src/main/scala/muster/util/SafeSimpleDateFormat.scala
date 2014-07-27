package muster
package util

import java.text.{ParsePosition, FieldPosition, SimpleDateFormat, DateFormat}
import java.util.{Date, Locale}

import muster.jackson.util.ISO8601DateFormat

/** Companion object for [[muster.util.SafeSimpleDateFormat]] that contains some defaults
  *
  */
object SafeSimpleDateFormat {
  /** The default locale for the system in the category format */
  val DefaultLocale = Locale.getDefault(Locale.Category.FORMAT)

  /** Lenient Iso8601 date format parser, accepts the pattern yyyy-MM-ddThh:mm:ss[.sss][Z|[+-]hh:mm] */
  val Iso8601Formatter: DateFormat = new ISO8601DateFormat
}

/** Provides a thread-safe version of the java.text.DateFormat interface
  *
  * It gets its thread-safety by using a thread-local to store the simple date format instance.
  * @param pattern The pattern to match
  * @param locale The locale to use when matching a pattern
  */
class SafeSimpleDateFormat(pattern: String, locale: Locale = SafeSimpleDateFormat.DefaultLocale) extends DateFormat {
  private[this] val df = new ThreadLocal[SimpleDateFormat] {
    override def initialValue(): SimpleDateFormat = new SimpleDateFormat(pattern, locale)
  }

  def format(date: Date, toAppendTo: StringBuffer, fieldPosition: FieldPosition): StringBuffer =
    df.get.format(date, toAppendTo, fieldPosition)

  def parse(source: String, pos: ParsePosition): Date = df.get.parse(source, pos)
}