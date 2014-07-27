package muster

import java.io.{PrintWriter, ByteArrayOutputStream}

object Appendable {

  /**
   * An appendable backed by a string builder
   * @param sb the string builder backing this appendable.
   */
   implicit class StringBuilderAppendable(sb: StringBuilder) extends  Appendable[StringBuilder] {
      def append(s: String): Appendable[StringBuilder] = {
       sb.append(s)
       this
     }
     def append(c: Char): Appendable[StringBuilder] = {
       sb.append(c)
       this
     }
     def close() { sb.clear() }
     def flush() { }

     override def toString: String = sb.toString()
   }

  /**
   * An appendable backed by a java.io.Writer
   * @param sb the writer backing this appendable
   */
   implicit class WriterAppendable(sb: java.io.Writer) extends  Appendable[java.io.Writer] {
     def append(s: String): Appendable[java.io.Writer] = {
       sb.write(s)
       this
     }
     def append(c: Char): Appendable[java.io.Writer] = {
       sb.append(c)
       this
     }
     def close() { sb.close() }
     def flush() { sb.flush() }

     override def toString: String = sb.toString
   }

   implicit class ByteArrayAppendable(arr: Array[Byte]) extends Appendable[Array[Byte]] {
     private[this] val strm = new PrintWriter(new ByteArrayOutputStream(), true)
     def append(s: String): Appendable[Array[Byte]] = {
       strm.append(s)
       this
     }

     def flush() {
       strm.flush()
     }

     def append(c: Char): Appendable[Array[Byte]] = {
       strm.append(c)
       this
     }

     def close(){
       strm.close()
     }
   }
 }

/**
 * An Appendable is an abstraction over a writer and other things that might
 * side effect when writing. Appendables only produce string values
 *
 * @tparam T the type of output sources this appender uses
 */
trait Appendable[T] extends AutoCloseable {
  /**
   * Append a string
   * @param s the string to append
   * @return return the underlying object
   */
  def append(s: String): Appendable[T]
  def append(c: Char): Appendable[T]
  def flush()
}