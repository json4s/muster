package muster

import java.io.{PrintWriter, ByteArrayOutputStream}
import java.nio.ByteBuffer

object Appendable {

  /**
   * An appendable backed by a string builder
   * @param sb the string builder backing this appendable.
   */
  implicit class StringBuilderAppendable(sb: StringBuilder) extends Appendable[StringBuilder, String] {
    def append(s: String): Appendable[StringBuilder, String] = {
      sb.append(s)
      this
    }

    def append(c: Char): Appendable[StringBuilder, String] = {
      sb.append(c)
      this
    }

    def close() {
      sb.clear()
    }

    def flush() {}


    def result(): String = sb.toString()

    override def toString: String = sb.toString()
  }

  /**
   * An appendable backed by a java.io.Writer
   * @param sb the writer backing this appendable
   */
  implicit class WriterAppendable(sb: java.io.Writer) extends Appendable[java.io.Writer, Unit] {
    def append(s: String): Appendable[java.io.Writer, Unit] = {
      sb.write(s)
      this
    }

    def append(c: Char): Appendable[java.io.Writer, Unit] = {
      sb.append(c)
      this
    }

    def close() {
      sb.close()
    }

    def flush() {
      sb.flush()
    }

    def result(): Unit = ()
  }

  implicit class ByteArrayAppendable(baos: ByteArrayOutputStream) extends Appendable[Array[Byte], Array[Byte]] {
    private[this] val strm = new PrintWriter(baos, true)

    def append(s: String): Appendable[Array[Byte], Array[Byte]] = {
      strm.append(s)
      this
    }

    def flush() {
      strm.flush()
    }

    def append(c: Char): Appendable[Array[Byte], Array[Byte]] = {
      strm.append(c)
      this
    }

    def close() {
      strm.close()
    }

    def result(): Array[Byte] = baos.toByteArray
  }

  implicit class ByteBufferAppendable(baos: ByteArrayOutputStream) extends Appendable[ByteBuffer, ByteBuffer] {
    private[this] val strm = new PrintWriter(baos, true)

    def result(): ByteBuffer = ByteBuffer.wrap(baos.toByteArray)

    def append(s: String): Appendable[ByteBuffer, ByteBuffer] = {
      strm.append(s)
      this
    }

    def flush() {
      strm.flush()
    }

    def append(c: Char): Appendable[ByteBuffer, ByteBuffer] = {
      strm.append(c)
      this
    }

    def close() {
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
trait Appendable[T, R] extends AutoCloseable {
  /**
   * Append a string
   * @param s the string to append
   * @return return the underlying object
   */
  def append(s: String): Appendable[T, R]
  def append(c: Char): Appendable[T, R]
  def flush()
  def result(): R
}