package muster


object MapKeySerializer {

  implicit object StringMapKeySerializer extends MapKeySerializer[String] {
    def deserialize(v: String): String = v
    def serialize(v: String): String = v
  }
  implicit object IntMapKeySerializer extends MapKeySerializer[Int] {
    def serialize(v: Int): String = v.toString
    def deserialize(v: String): Int = v.toInt
  }
  implicit object LongMapKeySerializer extends MapKeySerializer[Long] {
    def serialize(v: Long): String = v.toString
    def deserialize(v: String): Long = v.toLong
  }
}
trait MapKeySerializer[T] {
  def serialize(v: T): String
  def deserialize(v: String): T
}