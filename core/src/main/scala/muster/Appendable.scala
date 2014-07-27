package muster

import java.io.{PrintWriter, ByteArrayOutputStream}
import java.nio.{CharBuffer, ByteBuffer}
import java.nio.channels.WritableByteChannel

object Appendable {

  /** Creates an appendable for a string result */
  def forString(sb: StringBuilder = new StringBuilder): Appendable[String] = new StringBuilderAppendable(sb)

  /** Creates an appendable for a writer */
  def forWriter(writer: java.io.Writer): Appendable[Unit] = new WriterAppendable(writer)

  /** Creates an appendable for a byte array */
  def forByteArray(baos: ByteArrayOutputStream = new ByteArrayOutputStream()): Appendable[Array[Byte]] =
    new ByteArrayAppendable(baos)

  /** Creates an appenable for a byte buffer */
  def forByteBuffer(baos: ByteArrayOutputStream = new ByteArrayOutputStream()): Appendable[ByteBuffer] =
    new ByteBufferAppendable(baos)

  /** Creates appenable for a writable byte channel */
  def forWritableByteChannel(ch: WritableByteChannel): Appendable[Unit] =
    new WritableByteChannelAppendable(ch)

  /**
   * An appendable backed by a string builder
   * @param sb the string builder backing this appendable.
   */
  private final class StringBuilderAppendable(sb: StringBuilder) extends Appendable[String] {
    def append(s: String): Appendable[String] = {
      sb.append(s)
      this
    }

    def append(c: Char): Appendable[String] = {
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
  private final class WriterAppendable(sb: java.io.Writer) extends Appendable[Unit] {
    def append(s: String): Appendable[Unit] = {
      sb.write(s)
      this
    }

    def append(c: Char): Appendable[Unit] = {
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

  private final class ByteArrayAppendable(baos: ByteArrayOutputStream) extends Appendable[Array[Byte]] {
    private[this] val strm = new PrintWriter(baos, true)

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

    def close() {
      strm.close()
    }

    def result(): Array[Byte] = baos.toByteArray
  }

  private final class ByteBufferAppendable(baos: ByteArrayOutputStream) extends Appendable[ByteBuffer] {
    private[this] val strm = new PrintWriter(baos, true)

    def result(): ByteBuffer = ByteBuffer.wrap(baos.toByteArray)

    def append(s: String): Appendable[ByteBuffer] = {
      strm.append(s)
      this
    }

    def flush() {
      strm.flush()
    }

    def append(c: Char): Appendable[ByteBuffer] = {
      strm.append(c)
      this
    }

    def close() {
      strm.close()
    }
  }

  private final class WritableByteChannelAppendable(value: WritableByteChannel) extends Appendable[Unit] {
    def append(s: String): Appendable[Unit] = {
      value.write(scala.io.Codec.UTF8.encoder.encode(CharBuffer.wrap(s)))
      this
    }
    def result() {}

    def flush() {}

    def append(c: Char): Appendable[Unit] = {
      value.write(scala.io.Codec.UTF8.encoder.encode(CharBuffer.wrap(Array(c))))
      this
    }

    def close() { value.close() }
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
  def result(): T
}