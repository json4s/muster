package muster

object Appendable {

   implicit class StringBuilderAppendable(sb: StringBuilder) extends  Appendable[StringBuilder] {
     def append(s: String): StringBuilder = sb.append(s)
     def append(c: Char): StringBuilder = sb.append(c)
     def close() { sb.clear() }
     def flush() { }
   }

   implicit class WriterAppendable(sb: java.io.Writer) extends  Appendable[java.io.Writer] {
     def append(s: String): java.io.Writer = sb.append(s)
     def append(c: Char): java.io.Writer = sb.append(c)
     def close() { sb.close() }
     def flush() { sb.flush() }
   }
 }

trait Appendable[T] extends AutoCloseable {
  def append(s: String): T
  def append(c: Char): T
  def flush()
}