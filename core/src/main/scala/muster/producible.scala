package muster

import java.io._
import java.nio.ByteBuffer
import java.nio.channels.{WritableByteChannel, Channels}

import muster.Appendable.ByteArrayAppendable

import scala.annotation.implicitNotFound

object Producible {

  import scala.language.implicitConversions

  implicit def fileProducible(value: File) = FileProducible(value)

  implicit def writerProducible(value: java.io.Writer) = WriterProducible(value)

  implicit def outputStreamProducible(value: java.io.OutputStream) = OutputStreamProducible(value)
}

@implicitNotFound("Couldn't find a producible for ${T}. Try importing muster._ or to implement a muster.Producible")
trait Producible[T, R] {
  def value: T
  def toAppendable: Appendable[_, R]
}

final case class FileProducible(value: File) extends Producible[File, Unit] {
  def toAppendable: Appendable[java.io.Writer, Unit] = new FileWriter(value)
}

case object StringProducible extends Producible[String, String] {
  def value: String = ???
  def toAppendable: Appendable[StringBuilder, String] = new StringBuilder
}

final case class WriterProducible(value: java.io.Writer) extends Producible[java.io.Writer, Unit] {
  def toAppendable: Appendable[java.io.Writer, Unit] = value
}

final case class OutputStreamProducible(value: java.io.OutputStream) extends Producible[java.io.OutputStream, Unit] {
  def toAppendable: Appendable[java.io.Writer, Unit] = new PrintWriter(value, true)
}

case object ByteArrayProducible extends Producible[Array[Byte], Array[Byte]] {

  def value: Array[Byte] = ???

  def toAppendable: Appendable[Array[Byte], Array[Byte]] = new ByteArrayOutputStream()
}

case object ByteBufferProducible extends Producible[ByteBuffer, ByteBuffer] {

  def value: ByteBuffer = ???

  def toAppendable: Appendable[ByteBuffer, ByteBuffer] = new ByteArrayOutputStream()
}
