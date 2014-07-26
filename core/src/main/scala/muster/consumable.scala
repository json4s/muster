package muster

import java.nio.charset.Charset
import java.nio.{CharBuffer, ByteBuffer}
import java.nio.channels.ReadableByteChannel

import scala.annotation.implicitNotFound
import scala.io.Codec

/**
 * The Consumable companion object contains the default implicit conversions
 */
object Consumable {
  import scala.language.implicitConversions
  
  /** Creates a file consumable from a file */
  implicit def fileConsumable(value: java.io.File): Consumable[java.io.File] = FileConsumable(value)

  /** Creates a string consumable from a string */
  implicit def stringConsumable(value: String): Consumable[String] = StringConsumable(value)

  /** Creates an input stream consumable from an input stream */
  implicit def inputStreamConsumable(value: java.io.InputStream): Consumable[java.io.InputStream] = InputStreamConsumable(value)

  /** Creates a reader consumable from a reader */
  implicit def readerConsumable(value: java.io.Reader): Consumable[java.io.Reader] = ReaderConsumable(value)

  /** Creates a byte buffer consumable from a byte buffer */
  implicit def byteArrayConsumable(value: Array[Byte]): Consumable[Array[Byte]] = ByteArrayConsumable(value)

  /** Creates a URL consumable from a URL */
  implicit def urlConsumable(value: java.net.URL): Consumable[java.net.URL] = URLConsumable(value)

  /** Creates a file consumable from a file */
  implicit def byteChannelConsumable(value: ReadableByteChannel): Consumable[ReadableByteChannel] = ByteChannelConsumable(value)

  /** Creates a file consumable from a file */
  implicit def byteBufferConsumable(value: ByteBuffer): Consumable[ByteBuffer] = ByteBufferConsumable(value)

  /** Creates an input stream from a byte buffer, used with libraries that don't support nio or byte buffers */
  def byteBufferInputStream(buffer: ByteBuffer): java.io.InputStream = new ByteBufferInputStream(buffer)

  private final class ByteBufferInputStream(buffer: ByteBuffer) extends java.io.InputStream {
    @throws(classOf[java.io.IOException])
    def read(): Int = {
      if (!buffer.hasRemaining) {
          -1
      } else {
        buffer.get() & 0xFF
      }
    }

    override def read(b: Array[Byte], off: Int, len: Int): Int = {
      if (!buffer.hasRemaining) {
        -1
      } else {
        val l = len min buffer.remaining()
        buffer.get(b, off, l)
        l
      }
    }
  }

  def readerChannel(reader: java.io.Reader, charset: Charset = Codec.UTF8.charSet): ReadableByteChannel =
    new ReaderReadableChannel(reader, charset)

  private final class ReaderReadableChannel(reader: java.io.Reader, charset: Charset) extends ReadableByteChannel {
    private[this] var opened = true
    def read(dst: ByteBuffer): Int = {
      val buf = CharBuffer.allocate(dst.remaining())
      val len = reader.read(buf)
      charset.decode(dst)
      len
    }

    def isOpen: Boolean = opened

    def close(): Unit = {
      opened = false
      reader.close()
    }
  }
}

/**
 * A Consumable abstracts over various input sources and allows for picking a particular type of adapter
 * based on the type of the concrete consumer
 *
 * @tparam T the type of resource to read from
 */
@implicitNotFound("Couldn't find a consumable for ${T}. Try importing muster._ or to implement a muster.Consumable")
trait Consumable[T] {
  def value: T
}

/**
 * A file consumable wraps a file
 * @param value the [[java.io.File]] to read from
 */
final case class FileConsumable(value: java.io.File) extends Consumable[java.io.File]

/**
 * A string consumable wraps a string to read
 * @param value the string to read from
 */
final case class StringConsumable(value: String) extends Consumable[String]

/**
 * An input stream consumable wraps an input stream
 *
 * @param value the [[java.io.InputStream]]] to read from
 */
final case class InputStreamConsumable(value: java.io.InputStream) extends Consumable[java.io.InputStream]

/**
 * A reader consumable wraps a reader
 *
 * @param value the [[java.io.Reader]]] to read from
 */
final case class ReaderConsumable(value: java.io.Reader) extends Consumable[java.io.Reader]

/**
 * A byte array consumable wraps a byte array to read
 *
 * @param value the byte array to read from
 */
final case class ByteArrayConsumable(value: Array[Byte]) extends Consumable[Array[Byte]]

/**
 * A URL consumable reads from a url
 *
 * @param value the [[java.net.URL]]] to read from
 */
final case class URLConsumable(value: java.net.URL) extends Consumable[java.net.URL]

/**
 * A readable byte channel consumable wraps a readable byte channel
 *
 * @param value the byte channel to read from
 */
final case class ByteChannelConsumable(value: ReadableByteChannel) extends Consumable[ReadableByteChannel]

/**
 * A byte buffer consumable wraps a byte buffer to read
 *
 * @param value the byte buffer to read from
 */
final case class ByteBufferConsumable(value: ByteBuffer) extends Consumable[ByteBuffer]
