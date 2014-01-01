package muster

import scala.collection.immutable.BitSet
import scala.collection.generic.Growable

class ParseException(msg: String) extends Exception(msg)
class MappingException(msg: String) extends Exception(msg)

object Ast {
  // This no longer seems to be a problem, if it comes back will put the hack back in
  //private[this] val BrokenDouble = BigDecimal("2.2250738585072012e-308")

  sealed trait AstNode
  case object UndefinedNode extends AstNode
  case object NullNode extends AstNode
  trait BoolNode extends AstNode {
    def value: Boolean
  }
  case object TrueNode extends BoolNode {
    val value = true
  }
  case object FalseNode extends BoolNode {
    val value = false
  }

  case class ByteNode(value: Byte) extends AstNode
  case class ShortNode(value: Short) extends AstNode
  case class IntNode(value: Int) extends AstNode
  case class LongNode(value: Long) extends AstNode
  case class BigIntNode(value: BigInt) extends AstNode
  case class FloatNode(value: Float) extends AstNode
  case class DoubleNode(value: Double) extends AstNode
  case class BigDecimalNode(value: BigDecimal) extends AstNode

  case class NumberNode(value: String) extends AstNode {
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

  case class TextNode(value: CharSequence) extends AstNode

  abstract class ArrayNode(cursor: AstCursor) extends AstNode with AstCursor
  abstract class ObjectNode(cursor: AstCursor) extends AstNode with CursorFailures {
    def readArrayField(fieldName: String): ArrayNode
    def readObjectField(fieldName: String): ObjectNode
    def readStringField(fieldName: String): TextNode
    def readBooleanField(fieldName: String): BoolNode
    def readNumberField(fieldName: String): NumberNode
    def readByteField(fieldName: String): ByteNode
    def readShortField(fieldName: String): ShortNode
    def readIntField(fieldName: String): IntNode
    def readLongField(fieldName: String): LongNode
    def readBigIntField(fieldName: String): BigIntNode
    def readFloatField(fieldName: String): FloatNode
    def readDoubleField(fieldName: String): DoubleNode
    def readBigDecimalField(fieldName: String): BigDecimalNode
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
  def readArray(): ArrayNode
  def readObject(): ObjectNode
  def readString(): TextNode
  def readBoolean(): BoolNode
  def readNumber(): NumberNode
  def readByte(): ByteNode
  def readShort(): ShortNode
  def readInt(): IntNode
  def readLong(): LongNode
  def readBigInt(): BigIntNode
  def readFloat(): FloatNode
  def readDouble(): DoubleNode
  def readBigDecimal(): BigDecimalNode

  def nextNode(): AstNode


}
trait InputCursor[R] extends AstCursor {
  import InputCursor._
  def source: R
}

trait InputFormat[R] {
  type Cursor <: InputCursor[R]
  def createCursor(in: R): Cursor
}