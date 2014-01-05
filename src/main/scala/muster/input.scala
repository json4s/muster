package muster

import scala.collection.immutable.BitSet
import java.text._
import java.util.{Locale, Date}
import scala.util.Try
import scala.reflect.ClassTag
import com.fasterxml.jackson.databind.util.ISO8601DateFormat

class ParseException(msg: String) extends Exception(msg)

class MappingException(msg: String) extends Exception(msg)

object SafeSimpleDateFormat {
  val DefaultLocale = Locale.getDefault(Locale.Category.FORMAT)
  val Iso8601Formatter: DateFormat = new ISO8601DateFormat
}

class SafeSimpleDateFormat(pattern: String, locale: Locale = SafeSimpleDateFormat.DefaultLocale) extends DateFormat {
  private[this] val df = new ThreadLocal[SimpleDateFormat] {
    override def initialValue(): SimpleDateFormat = new SimpleDateFormat(pattern, locale)
  }

  def format(date: Date, toAppendTo: StringBuffer, fieldPosition: FieldPosition): StringBuffer =
    df.get.format(date, toAppendTo, fieldPosition)

  def parse(source: String, pos: ParsePosition): Date = df.get.parse(source, pos)
}

object Ast {

  // This no longer seems to be a problem, if it comes back will put the hack back in
  //private[this] val BrokenDouble = BigDecimal("2.2250738585072012e-308")

  sealed trait AstNode[T] {
    def value: T
  }

  case object UndefinedNode extends AstNode[Unit] {
    def value: Unit = ()
  }

  case object NullNode extends AstNode[Null] {
    def value: Null = null
  }

  trait BoolNode extends AstNode[Boolean] {
    def value: Boolean
  }

  case object TrueNode extends BoolNode {
    val value = true
  }

  case object FalseNode extends BoolNode {
    val value = false
  }

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

    def toByteAst = ByteNode(value.toByte)

    def toShortAst = ShortNode(value.toShort)

    def toIntAst = IntNode(value.toInt)

    def toLongAst = LongNode(value.toLong)

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

    def readField(fieldName: String): Ast.AstNode[_]

    def keySet: Set[String]

    def keysIterator: Iterator[String]

  }

}

object InputCursor {
  val NumberChars = BitSet('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', 'e', 'E', '-', '+')
  val WhiteSpace = BitSet(' ', '\r', '\n', '\t')

  def isNumberChar(c: Char): Boolean = NumberChars(c)

  def isWhitespace(c: Char): Boolean = c == '\r' || c == '\n' || c == '\t' || c == ' '

  def isSpecialChar(c: Char): Boolean = c == '\b' || c == '\f' || c == '\n'
}

import Ast._

trait CursorFailures {
  protected def failParse(msg: String) = throw new ParseException(msg)

  protected def failStructure(msg: String) = throw new MappingException(msg)
}

trait AstCursor extends CursorFailures {
  def readArray(): ArrayNode = readArrayOpt().getOrElse(notFound(classOf[ArrayNode]))

  def readObject(): ObjectNode = readObjectOpt().getOrElse(notFound(classOf[ObjectNode]))

  def readString(): TextNode = readStringOpt().getOrElse(notFound(classOf[TextNode]))

  def readBoolean(): BoolNode = readBooleanOpt().getOrElse(notFound(classOf[BoolNode]))

  def readNumber(): NumberNode = readNumberOpt().getOrElse(notFound(classOf[NumberNode]))

  def readByte(): ByteNode = readByteOpt().getOrElse(notFound(classOf[ByteNode]))

  def readShort(): ShortNode = readShortOpt().getOrElse(notFound(classOf[ShortNode]))

  def readInt(): IntNode = readIntOpt().getOrElse(notFound(classOf[IntNode]))

  def readLong(): LongNode = readLongOpt().getOrElse(notFound(classOf[LongNode]))

  def readBigInt(): BigIntNode = readBigIntOpt().getOrElse(notFound(classOf[BigIntNode]))

  def readFloat(): FloatNode = readFloatOpt().getOrElse(notFound(classOf[FloatNode]))

  def readDouble(): DoubleNode = readDoubleOpt().getOrElse(notFound(classOf[DoubleNode]))

  def readBigDecimal(): BigDecimalNode = readBigDecimalOpt().getOrElse(notFound(classOf[BigDecimalNode]))

  def readArrayOpt(): Option[ArrayNode]

  def readObjectOpt(): Option[ObjectNode]

  def readStringOpt(): Option[TextNode]

  def readBooleanOpt(): Option[BoolNode]

  def readNumberOpt(): Option[NumberNode]

  def readByteOpt(): Option[ByteNode] = readNumberOpt().map(_.toByteAst)

  def readShortOpt(): Option[ShortNode] = readNumberOpt().map(_.toShortAst)

  def readIntOpt(): Option[IntNode] = readNumberOpt().map(_.toIntAst)

  def readLongOpt(): Option[LongNode] = readNumberOpt().map(_.toLongAst)

  def readBigIntOpt(): Option[BigIntNode] = readNumberOpt().map(_.toBigIntAst)

  def readFloatOpt(): Option[FloatNode] = readNumberOpt().map(_.toFloatAst)

  def readDoubleOpt(): Option[DoubleNode] = readNumberOpt().map(_.toDoubleAst)

  def readBigDecimalOpt(): Option[BigDecimalNode] = readNumberOpt().map(_.toBigDecimalAst)

  def hasNextNode: Boolean = false

  def nextNode(): AstNode[_]

  private[this] def notFound(node: Class[_]) = failStructure(s"Could not find element of ${node.getSimpleName}")

}

trait InputCursor[R] extends AstCursor {

  import InputCursor._

  def source: R
}

trait InputFormat[R, C <: InputCursor[_]] {
  def createCursor(in: R): C

  def from[T](source: R)(implicit consumer: Consumer[T]): T = {
    val cursor = createCursor(source)
    consumer.consume(cursor.nextNode())
  }

  def tryFrom[T](source: R)(implicit consumer: Consumer[T]): Try[T] = Try(from(source))

}