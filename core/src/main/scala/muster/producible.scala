package muster

import java.io._

object Producible {

  import scala.language.implicitConversions

  implicit def fileProducible(value: File) = FileProducible(value)

  implicit def writerProducible(value: java.io.Writer) = WriterProducible(value)

  implicit def outputStreamProducible(value: java.io.OutputStream) = OutputStreamProducible(value)

}

trait Producible[T, R] {
  def value: T
  def toWriter: java.io.Writer
}

final case class FileProducible(value: File) extends Producible[File, Unit] {
  def toWriter: Writer = new FileWriter(value)
}

case object StringProducible extends Producible[String, String] {
  def value: String = ???

  def toWriter: Writer = new java.io.StringWriter()
}

final case class WriterProducible(value: java.io.Writer) extends Producible[java.io.Writer, Unit] {
  def toWriter: Writer = value
}

final case class OutputStreamProducible(value: java.io.OutputStream) extends Producible[java.io.OutputStream, Unit] {
  def toWriter: Writer = new PrintWriter(value, true)
}

