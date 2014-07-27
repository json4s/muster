package muster

import java.io._
import java.nio.file.Paths
import java.nio.{CharBuffer, ByteBuffer}
import java.nio.channels.{FileChannel, WritableByteChannel, Channels}

import muster.Appendable.ByteArrayAppendable

import scala.annotation.implicitNotFound

object Producible {

  import scala.language.implicitConversions

  implicit def fileProducible(value: File) = FileProducible(value)

  implicit def writerProducible(value: java.io.Writer) = WriterProducible(value)

  implicit def outputStreamProducible(value: java.io.OutputStream) = OutputStreamProducible(value)

  implicit def byteChannelProducible(value: WritableByteChannel) = WritableByteChannelProducible(value)
}

@implicitNotFound("Couldn't find a producible for ${T}. Try importing muster._ or to implement a muster.Producible")
trait Producible[T, R] {
  def value: T
  def toAppendable: Appendable[R]
}

final case class FileProducible(value: File) extends Producible[File, Unit] {
  def toAppendable: Appendable[Unit] = Appendable.forWritableByteChannel(FileChannel.open(Paths.get(value.getAbsoluteFile.toURI)))
}

case object StringProducible extends Producible[String, String] {
  def value: String = ???
  def toAppendable: Appendable[String] = Appendable.forString()
}

final case class WriterProducible(value: java.io.Writer) extends Producible[java.io.Writer, Unit] {
  def toAppendable: Appendable[Unit] = Appendable.forWriter(value)
}

final case class OutputStreamProducible(value: java.io.OutputStream) extends Producible[java.io.OutputStream, Unit] {
  def toAppendable: Appendable[Unit] = Appendable.forWriter(new PrintWriter(value, true))
}

case object ByteArrayProducible extends Producible[Array[Byte], Array[Byte]] {

  def value: Array[Byte] = ???

  def toAppendable: Appendable[Array[Byte]] = Appendable.forByteArray()
}

case object ByteBufferProducible extends Producible[ByteBuffer, ByteBuffer] {

  def value: ByteBuffer = ???

  def toAppendable: Appendable[ByteBuffer] = Appendable.forByteBuffer()
}

final case class WritableByteChannelProducible(value: WritableByteChannel) extends Producible[WritableByteChannel, Unit] {
  def toAppendable: Appendable[Unit] = Appendable.forWritableByteChannel(value)
}
