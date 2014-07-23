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
  def toAppendable: Appendable[_]
}

final case class FileProducible(value: File) extends Producible[File, Unit] {
  def toAppendable: Appendable[java.io.Writer] = new FileWriter(value)
}

case object StringProducible extends Producible[String, String] {
  def value: String = ???

  def toAppendable: Appendable[StringBuilder] = new StringBuilder
}

final case class WriterProducible(value: java.io.Writer) extends Producible[java.io.Writer, Unit] {
  def toAppendable: Appendable[java.io.Writer] = value
}

final case class OutputStreamProducible(value: java.io.OutputStream) extends Producible[java.io.OutputStream, Unit] {
  def toAppendable: Appendable[java.io.Writer] = new PrintWriter(value, true)
}

