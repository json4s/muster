package muster

import scala.util.control.NoStackTrace

/** The location of a parser
 *
 * @param line
 * @param col
 * @param source
 */
case class ParseLocation(line: Int, col: Int, source: Option[String] = None) {
  override def toString: String = {
    s"[Source: ${source.getOrElse("UNKNOWN")}, line: $line, col: $col]"
  }
}
class ParseException(msg: String, location: Option[ParseLocation]) extends Throwable(msg) {
  override def getMessage: String = {
    s"${Option(msg).getOrElse("N/A")} at $location"
  }
}


class MappingException(msg: String) extends Throwable(msg)
class EndOfInput extends Throwable("end of input") with NoStackTrace