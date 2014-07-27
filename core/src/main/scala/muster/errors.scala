package muster

import scala.util.control.NoStackTrace

/** The location of a parser when some interesting event occurs
 *
 * @param line the current line the parser is on
 * @param col the column the parser is on
 * @param source the source path if any
 */
case class ParseLocation(line: Int, col: Int, source: Option[String] = None) {
  override def toString: String = {
    s"[Source: ${source.getOrElse("UNKNOWN")}, line: $line, col: $col]"
  }
}

/** thrown when an error occurs during parsing of a stream
  *
  * @param msg the message for the error
  * @param location the location in the stream where this occurred
  */
class ParseException(msg: String, location: Option[ParseLocation]) extends Throwable(msg) {
  override def getMessage: String = {
    s"${Option(msg).getOrElse("N/A")} at $location"
  }
}

/** thrown when there is a problem mapping a value from or to a [[muster.ast.AstNode]] */
class MappingException(msg: String) extends Throwable(msg)

/** thrown when there is no more input to be gotten */
class EndOfInput extends Throwable("end of input") with NoStackTrace