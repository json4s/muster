package muster

import scala.collection.immutable.BitSet
import scala.collection.generic.Growable
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormatter}
import org.joda.time.DateTimeZone
import java.text._
import java.util.{Locale, Date}
import muster.Ast.IntNode
import muster.Ast.LongNode
import muster.Ast.BigDecimalNode
import muster.Ast.TextNode
import muster.Ast.FloatNode
import muster.Ast.BigIntNode
import muster.Ast.ShortNode
import muster.Ast.ByteNode
import muster.Ast.DoubleNode
import muster.Ast.NumberNode

class ParseException(msg: String) extends Exception(msg)
class MappingException(msg: String) extends Exception(msg)

//object SafeSimpleDateFormat {
//  val DefaultLocale =  Locale.getDefault(Locale.Category.FORMAT))
//}
//class SafeSimpleDateFormat(pattern: String, locale: Locale = SafeSimpleDateFormat.DefaultLocale) extends DateFormat {
//  private[this] val df = new ThreadLocal[SimpleDateFormat] {
//    override def initialValue(): SimpleDateFormat = new SimpleDateFormat(pattern, locale)
//  }
//
//  def format(date: Date, toAppendTo: StringBuffer, fieldPosition: FieldPosition): StringBuffer =
//    df.get.format(date, toAppendTo, fieldPosition)
//
//  def parse(source: String, pos: ParsePosition): Date = df.get.parse(source, pos)
//}
//
//trait DateFormatter {
//  def parse(s: String): Option[Date]
//  def format(d: Date)
//}

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

  case class ByteNode(value: Byte) extends AstNode[Byte]
  case class ShortNode(value: Short) extends AstNode[Short]
  case class IntNode(value: Int) extends AstNode[Int]
  case class LongNode(value: Long) extends AstNode[Long]
  case class BigIntNode(value: BigInt) extends AstNode[BigInt]
  case class FloatNode(value: Float) extends AstNode[Float]
  case class DoubleNode(value: Double) extends AstNode[Double]
  case class BigDecimalNode(value: BigDecimal) extends AstNode[BigDecimal]

  case class NumberNode(value: String) extends AstNode[String] {
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
    private[this] def notFound(fieldName: String) = failStructure(s"Could not find field $fieldName")
    def readArrayField(fieldName: String): ArrayNode =
      readArrayFieldOpt(fieldName).getOrElse(notFound(fieldName))
    def readObjectField(fieldName: String): ObjectNode =
      readObjectFieldOpt(fieldName).getOrElse(notFound(fieldName))
    def readStringField(fieldName: String): TextNode =
      readStringFieldOpt(fieldName).getOrElse(notFound(fieldName))
    def readBooleanField(fieldName: String): BoolNode =
      readBooleanFieldOpt(fieldName).getOrElse(notFound(fieldName))
    def readNumberField(fieldName: String): NumberNode =
      readNumberFieldOpt(fieldName).getOrElse(notFound(fieldName))
    def readByteField(fieldName: String): ByteNode =
      readByteFieldOpt(fieldName).getOrElse(notFound(fieldName))
    def readShortField(fieldName: String): ShortNode =
      readShortFieldOpt(fieldName).getOrElse(notFound(fieldName))
    def readIntField(fieldName: String): IntNode =
      readIntFieldOpt(fieldName).getOrElse(notFound(fieldName))
    def readLongField(fieldName: String): LongNode =
      readLongFieldOpt(fieldName).getOrElse(notFound(fieldName))
    def readBigIntField(fieldName: String): BigIntNode =
      readBigIntFieldOpt(fieldName).getOrElse(notFound(fieldName))
    def readFloatField(fieldName: String): FloatNode =
      readFloatFieldOpt(fieldName).getOrElse(notFound(fieldName))
    def readDoubleField(fieldName: String): DoubleNode =
      readDoubleFieldOpt(fieldName).getOrElse(notFound(fieldName))
    def readBigDecimalField(fieldName: String): BigDecimalNode =
      readBigDecimalFieldOpt(fieldName).getOrElse(notFound(fieldName))
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
    def readArrayFieldValue(fieldName: String): ArrayNode = readArrayField(fieldName)
    def readObjectFieldValue(fieldName: String): ObjectNode = readObjectField(fieldName)
    def readStringFieldValue(fieldName: String): String = readStringField(fieldName).value
    def readSymbolFieldValue(fieldName: String): Symbol = Symbol(readStringField(fieldName).value)
    def readBooleanFieldValue(fieldName: String): Boolean = readBooleanField(fieldName).value
    def readNumberFieldValue(fieldName: String): String = readNumberField(fieldName).value
    def readByteFieldValue(fieldName: String): Byte = readNumberField(fieldName).toByte
    def readJByteFieldValue(fieldName: String): java.lang.Byte = byte2Byte(readNumberField(fieldName).toByte)
    def readShortFieldValue(fieldName: String): Short = readNumberField(fieldName).toShort
    def readJShortFieldValue(fieldName: String): java.lang.Short = short2Short(readNumberField(fieldName).toShort)
    def readIntFieldValue(fieldName: String): Int = readNumberField(fieldName).toInt
    def readIntegerFieldValue(fieldName: String): java.lang.Integer = int2Integer(readNumberField(fieldName).toInt)
    def readLongFieldValue(fieldName: String): Long = readNumberField(fieldName).toLong
    def readJLongFieldValue(fieldName: String): java.lang.Long = long2Long(readNumberField(fieldName).toLong)
    def readBigIntFieldValue(fieldName: String): BigInt = readNumberField(fieldName).toBigInt
    def readBigIntegerFieldValue(fieldName: String): java.math.BigInteger = readNumberField(fieldName).toBigInt.bigInteger
    def readFloatFieldValue(fieldName: String): Float = readNumberField(fieldName).toFloat
    def readJFloatFieldValue(fieldName: String): java.lang.Float = float2Float(readNumberField(fieldName).toFloat)
    def readDoubleFieldValue(fieldName: String): Double = readNumberField(fieldName).toDouble
    def readJDoubleFieldValue(fieldName: String): java.lang.Double = double2Double(readNumberField(fieldName).toDouble)
    def readBigDecimalFieldValue(fieldName: String): BigDecimal = readNumberField(fieldName).toBigDecimal
    def readJBigDecimalFieldValue(fieldName: String): java.math.BigDecimal = readNumberField(fieldName).toBigDecimal.bigDecimal
    def readArrayFieldOptValue(fieldName: String): Option[ArrayNode] = readArrayFieldOpt(fieldName)
    def readObjectFieldOptValue(fieldName: String): Option[ObjectNode] = readObjectFieldOpt(fieldName)
    def readStringFieldOptValue(fieldName: String): Option[String] = readStringFieldOpt(fieldName).map(_.value)
    def readSymbolFieldOptValue(fieldName: String): Option[scala.Symbol] = readStringFieldOpt(fieldName).map(v => Symbol(v.value))
    def readBooleanFieldOptValue(fieldName: String): Option[Boolean] = readBooleanFieldOpt(fieldName).map(_.value)
    def readNumberFieldOptValue(fieldName: String): Option[String] = readNumberFieldOpt(fieldName).map(_.value)
    def readByteFieldOptValue(fieldName: String): Option[Byte] = readNumberFieldOpt(fieldName).map(_.toByte)
    def readJByteFieldOptValue(fieldName: String): Option[java.lang.Byte] = readNumberFieldOpt(fieldName).map(v => byte2Byte(v.toByte))
    def readShortFieldOptValue(fieldName: String): Option[Short] = readNumberFieldOpt(fieldName).map(_.toShort)
    def readJShortFieldOptValue(fieldName: String): Option[java.lang.Short] = readNumberFieldOpt(fieldName).map(v => short2Short(v.toShort))
    def readIntFieldOptValue(fieldName: String): Option[Int] = readNumberFieldOpt(fieldName).map(_.toInt)
    def readIntegerFieldOptValue(fieldName: String): Option[java.lang.Integer] = readNumberFieldOpt(fieldName).map(v => int2Integer(v.toInt))
    def readLongFieldOptValue(fieldName: String): Option[Long] = readNumberFieldOpt(fieldName).map(_.toLong)
    def readJLongFieldOptValue(fieldName: String): Option[java.lang.Long] = readNumberFieldOpt(fieldName).map(v => long2Long(v.toLong))
    def readBigIntFieldOptValue(fieldName: String): Option[BigInt] = readNumberFieldOpt(fieldName).map(_.toBigInt)
    def readBigIntegerFieldOptValue(fieldName: String): Option[java.math.BigInteger] = readNumberFieldOpt(fieldName).map(_.toBigInt.bigInteger)
    def readFloatFieldOptValue(fieldName: String): Option[Float] = readNumberFieldOpt(fieldName).map(_.toFloat)
    def readJFloatFieldOptValue(fieldName: String): Option[java.lang.Float] = readNumberFieldOpt(fieldName).map(v => float2Float(v.toFloat))
    def readDoubleFieldOptValue(fieldName: String): Option[Double] = readNumberFieldOpt(fieldName).map(_.toDouble)
    def readJDoubleFieldOptValue(fieldName: String): Option[java.lang.Double] = readNumberFieldOpt(fieldName).map(v => double2Double(v.toDouble))
    def readBigDecimalFieldOptValue(fieldName: String): Option[BigDecimal] = readNumberFieldOpt(fieldName).map(_.toBigDecimal)
    def readJBigDecimalFieldOptValue(fieldName: String): Option[java.math.BigDecimal] = readNumberFieldOpt(fieldName).map(_.toBigDecimal.bigDecimal)
    def readDateFieldValue(fieldName: String, dateFormat: DateTimeFormatter): java.util.Date =
      readDateTimeFieldValue(fieldName, dateFormat).toDate
    def readDateTimeFieldValue(fieldName: String, dateFormat: DateTimeFormatter): org.joda.time.DateTime =
      dateFormat.parseDateTime(readStringFieldValue(fieldName))
    def readDateFieldOptValue(fieldName: String, dateFormat: DateTimeFormatter): Option[java.util.Date] =
      readDateTimeFieldOptValue(fieldName, dateFormat).map(_.toDate)
    def readDateTimeFieldOptValue(fieldName: String, dateFormat: DateTimeFormatter): Option[org.joda.time.DateTime] =
      readStringFieldOptValue(fieldName) map dateFormat.parseDateTime
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
  def readArrayValue(): ArrayNode = readArray()
  def readObjectValue(): ObjectNode = readObject()
  def readStringValue(): String = readString().value
  def readSymbolValue(): Symbol = Symbol(readString().value)
  def readBooleanValue(): Boolean = readBoolean().value
  def readNumberValue(): String = readNumber().value
  def readByteValue(): Byte = readNumber().toByte
  def readJByteValue(): java.lang.Byte = byte2Byte(readNumber().toByte)
  def readShortValue(): Short = readNumber().toShort
  def readJShortValue(): java.lang.Short = short2Short(readNumber().toShort)
  def readIntValue(): Int = readNumber().toInt
  def readIntegerValue(): java.lang.Integer = int2Integer(readNumber().toInt)
  def readLongValue(): Long = readNumber().toLong
  def readJLongValue(): java.lang.Long = long2Long(readNumber().toLong)
  def readBigIntValue(): BigInt = readNumber().toBigInt
  def readBigIntegerValue(): java.math.BigInteger = readNumber().toBigInt.bigInteger
  def readFloatValue(): Float = readNumber().toFloat
  def readJFloatValue(): java.lang.Float = float2Float(readNumber().toFloat)
  def readDoubleValue(): Double = readNumber().toDouble
  def readJDoubleValue(): java.lang.Double = double2Double(readNumber().toDouble)
  def readBigDecimalValue(): BigDecimal = readNumber().toBigDecimal
  def readJBigDecimalValue(): java.math.BigDecimal = readNumber().toBigDecimal.bigDecimal
  def readArrayOptValue(): Option[ArrayNode] = readArrayOpt()
  def readObjectOptValue(): Option[ObjectNode] = readObjectOpt()
  def readStringOptValue(): Option[String] = readStringOpt().map(_.value)
  def readSymbolOptValue(): Option[Symbol] = readStringOpt().map(v => Symbol(v.value))
  def readBooleanOptValue(): Option[Boolean] = readBooleanOpt().map(_.value)
  def readNumberOptValue(): Option[String] = readNumberOpt().map(_.value)
  def readByteOptValue(): Option[Byte] = readNumberOpt().map(_.toByte)
  def readJByteOptValue(): Option[java.lang.Byte] = readNumberOpt().map(v => byte2Byte(v.toByte))
  def readShortOptValue(): Option[Short] = readNumberOpt().map(_.toShort)
  def readJShortOptValue(): Option[java.lang.Short] = readNumberOpt().map(v => short2Short(v.toShort))
  def readIntOptValue(): Option[Int] = readNumberOpt().map(_.toInt)
  def readIntegerOptValue(): Option[java.lang.Integer] = readNumberOpt().map(v => int2Integer(v.toInt))
  def readLongOptValue(): Option[Long] = readNumberOpt().map(_.toLong)
  def readJLongOptValue(): Option[java.lang.Long] = readNumberOpt().map(v => long2Long(v.toLong))
  def readBigIntOptValue(): Option[BigInt] = readNumberOpt().map(_.toBigInt)
  def readBigIntegerOptValue(): Option[java.math.BigInteger] = readNumberOpt().map(_.toBigInt.bigInteger)
  def readFloatOptValue(): Option[Float] = readNumberOpt().map(_.toFloat)
  def readJFloatOptValue(): Option[java.lang.Float] = readNumberOpt().map(v => float2Float(v.toFloat))
  def readDoubleOptValue(): Option[Double] = readNumberOpt().map(_.toDouble)
  def readJDoubleOptValue(): Option[java.lang.Double] = readNumberOpt().map(v => double2Double(v.toDouble))
  def readBigDecimalOptValue(): Option[BigDecimal] = readNumberOpt().map(_.toBigDecimal)
  def readJBigDecimalOptValue(): Option[java.math.BigDecimal] = readNumberOpt().map(_.toBigDecimal.bigDecimal)

  def readDateValue(dateFormat: DateTimeFormatter = Muster.from.JsonString.dateFormat): java.util.Date =
    readDateTimeValue(dateFormat).toDate
  def readDateTimeValue(dateFormat: DateTimeFormatter = Muster.from.JsonString.dateFormat): org.joda.time.DateTime =
    dateFormat.parseDateTime(readStringValue())
  def readDateOptValue(dateFormat: DateTimeFormatter = Muster.from.JsonString.dateFormat): Option[java.util.Date] =
    readDateTimeOptValue(dateFormat).map(_.toDate)
  def readDateTimeOptValue(dateFormat: DateTimeFormatter = Muster.from.JsonString.dateFormat): Option[org.joda.time.DateTime] =
    readStringOptValue() map dateFormat.parseDateTime


  def hasNextNode: Boolean = false
  def nextNode(): AstNode[_]
  private[this] def notFound(node: Class[_]) = failStructure(s"Could not find element of ${node.getSimpleName}")

}
trait InputCursor[R] extends AstCursor {
  import InputCursor._
  def source: R
}

trait InputFormat[R] {
  type Cursor <: InputCursor[R]
  type This <: InputFormat[R]
  def createCursor(in: R): Cursor
  def dateFormat: DateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis.withZone(DateTimeZone.UTC)
  def withDateFormat(df: DateTimeFormatter): This
}