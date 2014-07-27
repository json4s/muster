package muster

import scala.annotation.implicitNotFound

/**
 * Companion object with default map key serialzers
 */
object MapKeySerializer {

  /** A string key serializer, identity basically */
  implicit object StringMapKeySerializer extends MapKeySerializer[String] {
    def deserialize(v: String): String = v
    def serialize(v: String): String = v
  }

  /** A [[scala.Symbol]] key serializer */
  implicit object SymbolMapKeySerializer extends MapKeySerializer[Symbol] {
    def deserialize(v: String): Symbol = Symbol(v)
    def serialize(v: Symbol): String = v.name
  }

  /** An int key serializer */
  implicit object IntMapKeySerializer extends MapKeySerializer[Int] {
    def serialize(v: Int): String = v.toString
    def deserialize(v: String): Int = v.toInt
  }

  /** A long key serializer */
  implicit object LongMapKeySerializer extends MapKeySerializer[Long] {
    def serialize(v: Long): String = v.toString
    def deserialize(v: String): Long = v.toLong
  }
}

/** A map key serializer type class
  *
  * This is used to serialize keys for maps and map like values to and from string.
  * @tparam T
  */
@implicitNotFound("Couldn't find a map key serializer for ${T}, try to import muster._ or implement a muster.MapKeySerializer[${T}]")
trait MapKeySerializer[T] {
  /** Serialize the key from T to a string
    *
    * @param v the value to serialize
    * @return the string representation of this key
    */
  def serialize(v: T): String

  /** Deserialize the string into a key value
    *
    * @param v the string to deserialize
    * @return the value for use as a key
    */
  def deserialize(v: String): T
}