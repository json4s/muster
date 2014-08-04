package muster
package ast

import muster.input.{AstCursor, CursorFailures}

import scala.reflect.ClassTag

// This no longer seems to be a problem, if it comes back will put the hack back in
//private[this] val BrokenDouble = BigDecimal("2.2250738585072012e-308")

/** Represents an AST node in musters view of the world
  *
  * @tparam T the value type of this AST node
  */
sealed trait AstNode[T] {
  def value: T
}

/** Represents an undefined or missing entity */
case object UndefinedNode extends AstNode[Unit] {
  def value: Unit = ()
}

/** Represents a null entity */
case object NullNode extends AstNode[Null] {
  def value: Null = null
}

/** Represents a boolean entity */
trait BoolNode extends AstNode[Boolean] {
  def value: Boolean
}

/** Represents a true value */
case object TrueNode extends BoolNode {
  val value = true
}

/** Represents a false value */
case object FalseNode extends BoolNode {
  val value = false
}

/** Represents a byte entity
  *
  * @param value the value of this entity
  */
case class ByteNode(value: Byte) extends AstNode[Byte] with NumberNodeLike[Byte] {
  def toByte = value

  def toShort = value.toShort

  def toInt = value.toInt

  def toLong = value.toLong

  def toBigInt = BigInt(value)

  def toDouble = value.toDouble

  def toFloat = value.toFloat

  def toBigDecimal = BigDecimal(value)

  def toByteAst = ByteNode(value.toByte)

  def toShortAst = ShortNode(value.toShort)

  def toIntAst = IntNode(value.toInt)

  def toLongAst = LongNode(value.toLong)

  def toBigIntAst = BigIntNode(BigInt(value))

  def toDoubleAst = DoubleNode(value.toDouble)

  def toFloatAst = FloatNode(value.toFloat)

  def toBigDecimalAst = BigDecimalNode(BigDecimal(value))
}

case class ShortNode(value: Short) extends AstNode[Short] with NumberNodeLike[Short] {
  def toByte = value.toByte

  def toShort = value

  def toInt = value.toInt

  def toLong = value.toLong

  def toBigInt = BigInt(value)

  def toDouble = value.toDouble

  def toFloat = value.toFloat

  def toBigDecimal = BigDecimal(value)

  def toByteAst = ByteNode(value.toByte)

  def toShortAst = ShortNode(value.toShort)

  def toIntAst = IntNode(value.toInt)

  def toLongAst = LongNode(value.toLong)

  def toBigIntAst = BigIntNode(BigInt(value))

  def toDoubleAst = DoubleNode(value.toDouble)

  def toFloatAst = FloatNode(value.toFloat)

  def toBigDecimalAst = BigDecimalNode(BigDecimal(value))

}

case class IntNode(value: Int) extends AstNode[Int] with NumberNodeLike[Int] {
  def toByte = value.toByte

  def toShort = value.toShort

  def toInt = value.toInt

  def toLong = value.toLong

  def toBigInt = BigInt(value)

  def toDouble = value.toDouble

  def toFloat = value.toFloat

  def toBigDecimal = BigDecimal(value)

  def toByteAst = ByteNode(value.toByte)

  def toShortAst = ShortNode(value.toShort)

  def toIntAst = IntNode(value.toInt)

  def toLongAst = LongNode(value.toLong)

  def toBigIntAst = BigIntNode(BigInt(value))

  def toDoubleAst = DoubleNode(value.toDouble)

  def toFloatAst = FloatNode(value.toFloat)

  def toBigDecimalAst = BigDecimalNode(BigDecimal(value))

}

case class LongNode(value: Long) extends AstNode[Long] with NumberNodeLike[Long] {
  def toByte = value.toByte

  def toShort = value.toShort

  def toInt = value.toInt

  def toLong = value.toLong

  def toBigInt = BigInt(value)

  def toDouble = value.toDouble

  def toFloat = value.toFloat

  def toBigDecimal = BigDecimal(value)

  def toByteAst = ByteNode(value.toByte)

  def toShortAst = ShortNode(value.toShort)

  def toIntAst = IntNode(value.toInt)

  def toLongAst = LongNode(value.toLong)

  def toBigIntAst = BigIntNode(BigInt(value))

  def toDoubleAst = DoubleNode(value.toDouble)

  def toFloatAst = FloatNode(value.toFloat)

  def toBigDecimalAst = BigDecimalNode(BigDecimal(value))

}

case class BigIntNode(value: BigInt) extends AstNode[BigInt] with NumberNodeLike[BigInt] {
  def toByte = value.toByte

  def toShort = value.toShort

  def toInt = value.toInt

  def toLong = value.toLong

  def toBigInt = value

  def toDouble = value.toDouble

  def toFloat = value.toFloat

  def toBigDecimal = BigDecimal(value)

  def toByteAst = ByteNode(value.toByte)

  def toShortAst = ShortNode(value.toShort)

  def toIntAst = IntNode(value.toInt)

  def toLongAst = LongNode(value.toLong)

  def toBigIntAst = BigIntNode(value)

  def toDoubleAst = DoubleNode(value.toDouble)

  def toFloatAst = FloatNode(value.toFloat)

  def toBigDecimalAst = BigDecimalNode(BigDecimal(value))

}

case class FloatNode(value: Float) extends AstNode[Float] with NumberNodeLike[Float] {
  def toByte = value.toByte

  def toShort = value.toShort

  def toInt = value.toInt

  def toLong = value.toLong

  def toBigInt = BigDecimal(value.toDouble).toBigInt()

  def toDouble = value.toDouble

  def toFloat = value.toFloat

  def toBigDecimal = BigDecimal(value.toDouble)

  def toByteAst = ByteNode(value.toByte)

  def toShortAst = ShortNode(value.toShort)

  def toIntAst = IntNode(value.toInt)

  def toLongAst = LongNode(value.toLong)

  def toBigIntAst = BigIntNode(BigDecimal(value.toDouble).toBigInt())

  def toDoubleAst = DoubleNode(value.toDouble)

  def toFloatAst = FloatNode(value.toFloat)

  def toBigDecimalAst = BigDecimalNode(BigDecimal(value.toDouble))

}

case class DoubleNode(value: Double) extends AstNode[Double] with NumberNodeLike[Double] {
  def toByte = value.toByte

  def toShort = value.toShort

  def toInt = value.toInt

  def toLong = value.toLong

  def toBigInt = BigDecimal(value).toBigInt()

  def toDouble = value.toDouble

  def toFloat = value.toFloat

  def toBigDecimal = BigDecimal(value)

  def toByteAst = ByteNode(value.toByte)

  def toShortAst = ShortNode(value.toShort)

  def toIntAst = IntNode(value.toInt)

  def toLongAst = LongNode(value.toLong)

  def toBigIntAst = BigIntNode(BigDecimal(value).toBigInt())

  def toDoubleAst = DoubleNode(value.toDouble)

  def toFloatAst = FloatNode(value.toFloat)

  def toBigDecimalAst = BigDecimalNode(BigDecimal(value))

}

case class BigDecimalNode(value: BigDecimal) extends AstNode[BigDecimal] with NumberNodeLike[BigDecimal] {
  def toByte = value.toByte

  def toShort = value.toShort

  def toInt = value.toInt

  def toLong = value.toLong

  def toBigInt = value.toBigInt()

  def toDouble = value.toDouble

  def toFloat = value.toFloat

  def toBigDecimal = value

  def toByteAst = ByteNode(value.toByte)

  def toShortAst = ShortNode(value.toShort)

  def toIntAst = IntNode(value.toInt)

  def toLongAst = LongNode(value.toLong)

  def toBigIntAst = BigIntNode(value.toBigInt())

  def toDoubleAst = DoubleNode(value.toDouble)

  def toFloatAst = FloatNode(value.toFloat)

  def toBigDecimalAst = BigDecimalNode(value)

}

trait NumberNodeLike[T] {
  self: AstNode[T] =>
  def toByte: Byte

  def toShort: Short

  def toInt: Int

  def toLong: Long

  def toBigInt: BigInt

  def toDouble: Double

  def toFloat: Float

  def toBigDecimal: BigDecimal

  def toByteAst: ByteNode

  def toShortAst: ShortNode

  def toIntAst: IntNode

  def toLongAst: LongNode

  def toBigIntAst: BigIntNode

  def toDoubleAst: DoubleNode

  def toFloatAst: FloatNode

  def toBigDecimalAst: BigDecimalNode
}

case class NumberNode(value: String) extends AstNode[String] with NumberNodeLike[String] {
  def toByte = value.toByte

  def toShort = value.toShort

  def toInt = value.toInt

  def toLong = value.toLong

  def toBigInt = BigInt(value)

  def toDouble = value.toDouble

  def toFloat = value.toFloat

  def toBigDecimal = BigDecimal(value)

  def toByteAst = ByteNode(value.toDouble.toByte)

  def toShortAst = ShortNode(value.toDouble.toShort)

  def toIntAst = IntNode(value.toDouble.toInt)

  def toLongAst = LongNode(value.toDouble.toLong)

  def toBigIntAst = BigIntNode(BigInt(value))

  def toDoubleAst = DoubleNode(value.toDouble)

  def toFloatAst = FloatNode(value.toFloat)

  def toBigDecimalAst = BigDecimalNode(BigDecimal(value))
}

case class TextNode(value: String) extends AstNode[String]

abstract class ArrayNode(val value: AstCursor) extends AstNode[AstCursor] with AstCursor

abstract class ObjectNode(val value: AstCursor) extends AstNode[AstCursor] with CursorFailures {
  private[this] def notFound[T](fieldName: String)(implicit ct: ClassTag[T]) = failStructure(s"Could not find field $fieldName as ${ct.runtimeClass.getSimpleName}")

  def readArrayField(fieldName: String): ArrayNode =
    readArrayFieldOpt(fieldName).getOrElse(notFound[ArrayNode](fieldName))

  def readObjectField(fieldName: String): ObjectNode =
    readObjectFieldOpt(fieldName).getOrElse(notFound[ObjectNode](fieldName))

  def readStringField(fieldName: String): TextNode =
    readStringFieldOpt(fieldName).getOrElse(notFound[TextNode](fieldName))

  def readBooleanField(fieldName: String): BoolNode =
    readBooleanFieldOpt(fieldName).getOrElse(notFound[BoolNode](fieldName))

  def readNumberField(fieldName: String): NumberNode =
    readNumberFieldOpt(fieldName).getOrElse(notFound[NumberNode](fieldName))

  def readByteField(fieldName: String): ByteNode =
    readByteFieldOpt(fieldName).getOrElse(notFound[ByteNode](fieldName))

  def readShortField(fieldName: String): ShortNode =
    readShortFieldOpt(fieldName).getOrElse(notFound[ShortNode](fieldName))

  def readIntField(fieldName: String): IntNode =
    readIntFieldOpt(fieldName).getOrElse(notFound[IntNode](fieldName))

  def readLongField(fieldName: String): LongNode =
    readLongFieldOpt(fieldName).getOrElse(notFound[LongNode](fieldName))

  def readBigIntField(fieldName: String): BigIntNode =
    readBigIntFieldOpt(fieldName).getOrElse(notFound[BigIntNode](fieldName))

  def readFloatField(fieldName: String): FloatNode =
    readFloatFieldOpt(fieldName).getOrElse(notFound[FloatNode](fieldName))

  def readDoubleField(fieldName: String): DoubleNode =
    readDoubleFieldOpt(fieldName).getOrElse(notFound[Double](fieldName))

  def readBigDecimalField(fieldName: String): BigDecimalNode =
    readBigDecimalFieldOpt(fieldName).getOrElse(notFound[BigDecimalNode](fieldName))

  def readArrayFieldOpt(fieldName: String): Option[ArrayNode]

  def readObjectFieldOpt(fieldName: String): Option[ObjectNode]

  def readStringFieldOpt(fieldName: String): Option[TextNode]

  def readBooleanFieldOpt(fieldName: String): Option[BoolNode]

  def readNumberFieldOpt(fieldName: String): Option[NumberNode]

  def readByteFieldOpt(fieldName: String): Option[ByteNode] = readNumberFieldOpt(fieldName).map(_.toByteAst)

  def readShortFieldOpt(fieldName: String): Option[ShortNode] = readNumberFieldOpt(fieldName).map(_.toShortAst)

  def readIntFieldOpt(fieldName: String): Option[IntNode] = readNumberFieldOpt(fieldName).map(_.toIntAst)

  def readLongFieldOpt(fieldName: String): Option[LongNode] = readNumberFieldOpt(fieldName).map(_.toLongAst)

  def readBigIntFieldOpt(fieldName: String): Option[BigIntNode] = readNumberFieldOpt(fieldName).map(_.toBigIntAst)

  def readFloatFieldOpt(fieldName: String): Option[FloatNode] = readNumberFieldOpt(fieldName).map(_.toFloatAst)

  def readDoubleFieldOpt(fieldName: String): Option[DoubleNode] = readNumberFieldOpt(fieldName).map(_.toDoubleAst)

  def readBigDecimalFieldOpt(fieldName: String): Option[BigDecimalNode] = readNumberFieldOpt(fieldName).map(_.toBigDecimalAst)

  def readField(fieldName: String): AstNode[_]

  def keySet: Set[String]

  def keysIterator: Iterator[String]

}