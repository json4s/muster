package muster

import scala.collection.mutable

object Quoter {
  object Appendable {
    implicit class StringBuilderAppendable(sb: mutable.StringBuilder) extends  Appendable[StringBuilder] {
      def append(s: String): mutable.StringBuilder = sb.append(s)
      def append(c: Char): mutable.StringBuilder = sb.append(c)
    }
    implicit class WriterAppendable(sb: java.io.Writer) extends  Appendable[java.io.Writer] {
      def append(s: String): java.io.Writer = sb.append(s)
      def append(c: Char): java.io.Writer = sb.append(c)
    }
  }
  trait Appendable[T] {
    def append(s: String): T
    def append(c: Char): T
  }

  def jsonQuote(s: String, writer: Appendable[_]) {
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