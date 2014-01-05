package muster

import java.io.File


object Consumable {

  import scala.language.implicitConversions

  implicit def fileConsumable(value: File) = FileConsumable(value)

  implicit def stringConsumable(value: String) = StringConsumable(value)

  implicit def readerConsumable(value: java.io.Reader) = ReaderConsumable(value)

  implicit def inputStreamConsumable(value: java.io.InputStream) = InputStreamConsumable(value)

  implicit def byteArrayConsumable(value: Array[Byte]) = ByteArrayConsumable(value)

  implicit def urlConsumable(value: java.net.URL) = URLConsumable(value)
}

trait Consumable[T] {
  def value: T
}

final case class FileConsumable(value: File) extends Consumable[File]

final case class StringConsumable(value: String) extends Consumable[String]

final case class ReaderConsumable(value: java.io.Reader) extends Consumable[java.io.Reader]

final case class InputStreamConsumable(value: java.io.InputStream) extends Consumable[java.io.InputStream]

final case class ByteArrayConsumable(value: Array[Byte]) extends Consumable[Array[Byte]]

final case class URLConsumable(value: java.net.URL) extends Consumable[java.net.URL]