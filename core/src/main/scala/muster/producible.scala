package muster

import java.io._
import java.nio.ByteBuffer
import java.nio.channels.{FileChannel, WritableByteChannel}
import java.nio.file.Paths

import scala.annotation.implicitNotFound

/** The companion object for a Producible, contains the default implicit conversions */
object Producible {

  import scala.language.implicitConversions

  /** Implicit conversion from a [[java.io.File]] to a [[muster.Producible]] */
  implicit def fileProducible(value: File) = FileProducible(value)

  /** Implicit conversion from a [[java.io.Writer]] to a [[muster.Producible]] */
  implicit def writerProducible(value: java.io.Writer) = WriterProducible(value)

  /** Implicit conversion from a [[java.io.OutputStream]] to a [[muster.Producible]] */
  implicit def outputStreamProducible(value: java.io.OutputStream) = OutputStreamProducible(value)

  /** Implicit conversion from a [[java.nio.channels.WritableByteChannel]] to a [[muster.Producible]] */
  implicit def byteChannelProducible(value: WritableByteChannel) = WritableByteChannelProducible(value)
}

/** Wraps an output target so that it can be used by a renderer
  *
  * @tparam T The type of output target this producible wraps
  * @tparam R The value it will result in
  */
@implicitNotFound("Couldn't find a producible for ${T}. Try importing muster._ or to implement a muster.Producible")
trait Producible[T, R] {
  def value: T
  def toAppendable: util.Appendable[R]
}

/** Wraps a file as an output target, renderers will produce their output into this file
  *
  * @param value The file to produce to
  */
final case class FileProducible(value: File) extends Producible[File, Unit] {
  def toAppendable: util.Appendable[Unit] = util.Appendable.forWritableByteChannel(FileChannel.open(Paths.get(value.getAbsoluteFile.toURI)))
}

/** Used to produce string values with a renderer */
case object StringProducible extends Producible[String, String] {
  def value: String = ???
  def toAppendable: util.Appendable[String] = util.Appendable.forString()
}

/** Used to render values onto a text stream
  *
  * @param value the writer to use when rendering values with a producer
  */
final case class WriterProducible(value: java.io.Writer) extends Producible[java.io.Writer, Unit] {
  def toAppendable: util.Appendable[Unit] = util.Appendable.forWriter(value)
}

/** Wraps an output stream as target for a renderer
  *
  * @param value the output stream to write to
  */
final case class OutputStreamProducible(value: java.io.OutputStream) extends Producible[java.io.OutputStream, Unit] {
  def toAppendable: util.Appendable[Unit] = util.Appendable.forWriter(new PrintWriter(value, true))
}

/** Used to produce a byte array with a renderer */
case object ByteArrayProducible extends Producible[Array[Byte], Array[Byte]] {

  def value: Array[Byte] = ???

  def toAppendable: util.Appendable[Array[Byte]] = util.Appendable.forByteArray()
}

/** Used to produce a ByteBffer with a renderer */
case object ByteBufferProducible extends Producible[ByteBuffer, ByteBuffer] {

  def value: ByteBuffer = ???

  def toAppendable: util.Appendable[ByteBuffer] = util.Appendable.forByteBuffer()
}

/** Wraps a writable byte channel as an output target, renderers will produce their output onto this channel
  *
  * @param value the channel to produce the output onto
  */
final case class WritableByteChannelProducible(value: WritableByteChannel) extends Producible[WritableByteChannel, Unit] {
  def toAppendable: util.Appendable[Unit] = util.Appendable.forWritableByteChannel(value)
}
