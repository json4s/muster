package muster

import muster.Ast._
import java.nio.{ByteBuffer, CharBuffer}
import scala.annotation.{tailrec, switch}
import scala.collection.mutable

trait JsonSourceIterator extends Iterator[Char] {
  def pos: Int
  def charAt(idx: Int): Char
  def current: Char
  def peek: Char
}

object JsonInputCursor {

  private[this] val notNextField = (c: Char) =>  { InputCursor.isWhitespace(c) || c == ',' }
  private[this] val notFieldValue = (c: Char) =>  { InputCursor.isWhitespace(c) || c == ':' }

  final case class JsonArrayNode(cursor: JsonInputCursor[_]) extends ArrayNode(cursor) with AstCursor {

    private[this] val eleName = "element"
    private[this] def iterator = cursor.iterator
    private[this] def skipToNextValue = iterator.takeWhile(notNextField)
    private[this] var fields = {
      val buf = Vector.newBuilder[AstNode]
      if (iterator.hasNext) iterator.next()
      else failParse(s"Unexpected character ${iterator.current} at ${iterator.pos}.")
      while(iterator.current != ']' && iterator.hasNext) {
        buf += cursor.nextNode()
        skipToNextValue
      }
      if (iterator.current != ']') failParse(s"Unexpected character when looking for ']' at ${iterator.pos}")
      buf.result()
    }

    def nextNode(): AstNode = if (hasNext) {
      val hd = fields.head
      fields = fields.tail
      hd
    } else failStructure("No more elements in the JsonArrayNode!")

    def hasNext = !fields.isEmpty

    private[this] def withNext[T <: AstNode](klass: Class[T]): T = {
      val nxt = nextNode()
      if (klass.isAssignableFrom(nxt.getClass)) nxt.asInstanceOf[T]
      else err(nxt, klass.getSimpleName)
    }

    private[this] def err(x: AstNode, nodeType: String) =
      failStructure(s"Expected to have an $nodeType $eleName but got ${x.getClass.getSimpleName}")

    def readArray(): ArrayNode = withNext(classOf[JsonArrayNode])
    def readObject(): ObjectNode = withNext(classOf[JsonObjectNode])
    def readString(): TextNode = withNext(classOf[TextNode])
    def readBoolean(): BoolNode = withNext(classOf[BoolNode])
    def readNumber(): NumberNode = withNext(classOf[NumberNode])
    def readByte(): ByteNode = withNext(classOf[ByteNode])
    def readShort(): ShortNode = withNext(classOf[ShortNode])
    def readInt(): IntNode = withNext(classOf[IntNode])
    def readLong(): LongNode = withNext(classOf[LongNode])
    def readBigInt(): BigIntNode = withNext(classOf[BigIntNode])
    def readFloat(): FloatNode = withNext(classOf[FloatNode])
    def readDouble(): DoubleNode = withNext(classOf[DoubleNode])
    def readBigDecimal(): BigDecimalNode = withNext(classOf[BigDecimalNode])
  }


  final case class JsonObjectNode(cursor: JsonInputCursor[_]) extends ObjectNode(cursor) {
    private[this] val eleName = "field"
    private[this] def iterator = cursor.iterator
    private[this] def skipToNextField = iterator.takeWhile(notNextField)
    private[this] def skipToFieldValue = iterator.takeWhile(notFieldValue)

    private[this] val fields = {
      val buf = mutable.HashMap.empty[String, AstNode]
      if (iterator.hasNext) iterator.next() // move past '{'
      else failParse(s"Unexpected character ${iterator.current} at ${iterator.pos}.")
      while(iterator.current != '}' && iterator.hasNext) {
        skipToNextField
        val field = cursor.readString()
        skipToFieldValue
        buf += field.value.toString -> cursor.nextNode()
        skipToNextField
      }
      if (iterator.current != '}') failParse(s"Unexpected character or end of input when looking for '}' at ${iterator.pos}")
      buf
    }

    def keys = fields.keys
    def keySet = fields.keySet

    private[this] def err(x: AstNode, fieldName: String, nodeType: Class[_]) =
      failStructure(s"Expected to have an ${nodeType.getSimpleName} $eleName for '$fieldName' but got ${x.getClass.getSimpleName}")

    private[this] def readField[T](fieldName: String, nodeType: Class[T]): T = {
      val fldOpt = fields.get(fieldName)
      if (fldOpt.isDefined) {
        val x = fldOpt.get
        if (nodeType.isAssignableFrom(x.getClass)) x.asInstanceOf[T]
        else err(x, fieldName, nodeType)
      } else failStructure(s"Could not find the field '$fieldName' in the json object")
    }

    def readArrayField(fieldName: String): ArrayNode = readField(fieldName, classOf[JsonArrayNode])
    def readObjectField(fieldName: String): ObjectNode = readField(fieldName, classOf[JsonObjectNode])
    def readStringField(fieldName: String): TextNode = readField(fieldName, classOf[TextNode])
    def readBooleanField(fieldName: String): BoolNode = readField(fieldName, classOf[BoolNode])
    def readNumberField(fieldName: String): NumberNode = readField(fieldName, classOf[NumberNode])
    def readByteField(fieldName: String): ByteNode = readField(fieldName, classOf[ByteNode])
    def readShortField(fieldName: String): ShortNode = readField(fieldName, classOf[ShortNode])
    def readIntField(fieldName: String): IntNode = readField(fieldName, classOf[IntNode])
    def readLongField(fieldName: String): LongNode = readField(fieldName, classOf[LongNode])
    def readBigIntField(fieldName: String): BigIntNode = readField(fieldName, classOf[BigIntNode])
    def readFloatField(fieldName: String): FloatNode = readField(fieldName, classOf[FloatNode])
    def readDoubleField(fieldName: String): DoubleNode = readField(fieldName, classOf[DoubleNode])
    def readBigDecimalField(fieldName: String): BigDecimalNode = readField(fieldName, classOf[BigDecimalNode])
  }
}
trait JsonInputCursor[R] extends InputCursor[R] {

  import JsonInputCursor._
  protected def size: Int
  protected def iterator: JsonSourceIterator

  def readArray(): ArrayNode = JsonArrayNode(this)
  def readObject(): ObjectNode = JsonObjectNode(this)

  def readString(): TextNode = {
    skipWhiteSpace()
    val end = iterator.current // store quote as separator
    var shouldContinue = iterator.hasNext
    var seenEnd = false
    val sb = new StringBuilder
    while(shouldContinue) {
      (iterator.next(): @switch) match {
        case '"' | '\'' =>
          if (end == iterator.current) {
            shouldContinue = false
            seenEnd = true
          } else sb.append(iterator.current)
        case '\\' =>
          if (!iterator.hasNext) throw failParse("Tried to move past the end of the input")
          else {
            (iterator.next(): @switch) match {
              case 't' => sb.append('\t')
              case 'n' => sb.append('\n')
              case 'r' => sb.append('\r')
              case 'f' => sb.append('\f')
              case 'b' => sb.append('\b')
              case '\\' => sb.append('\\')
              case '/' => sb.append('/')
              case '\'' => sb.append('\'')
              case '"' => sb.append('"')
              case 'u' => sb.append(readUnicode(4))
              case 'x' => sb.append(readUnicode(2))
              case _ =>
            }
            shouldContinue = iterator.hasNext
          }
        case '\0' | 1 | 2 | 3 | 4 | 5 | 6 | 7 | '\b' | '\t' | '\n' | 11 | '\f' | '\r' | 14 | 15 |
             16 | 17 | 18 | 19 | 20 | 21 | 22 | 23 | 24 | 25 | 27 | 28 | 29 | 30 | 31 | 127  =>
          throw failParse(s"Unexpected control char at ${iterator.pos}")
        case c =>
          sb.append(c)
          shouldContinue = iterator.hasNext
      }

    }
    if (!seenEnd) throw failParse(s"Input ended too early while trying to find a closing quote for a string at ${iterator.pos}")
    TextNode(sb.toString())
  }
  
  def readBoolean(): BoolNode = {
    skipWhiteSpace()
    if (iterator.current == 't' &&
        iterator.peek == 'r' &&
        iterator.charAt(iterator.pos + 2) == 'u' &&
        iterator.charAt(iterator.pos + 3) == 'e') TrueNode
    else if (iterator.current == 'f' &&
              iterator.peek == 'a' &&
              iterator.charAt(iterator.pos + 2) == 'l' &&
              iterator.charAt(iterator.pos + 3) == 's' &&
              iterator.charAt(iterator.pos + 4) == 'e') FalseNode
    else throw failParse(s"Unexpected char '${iterator.current}' at ${iterator.pos}")
  }
  
  def readNumber(): NumberNode = {
    skipWhiteSpace()
    NumberNode(iterator.current + iterator.takeWhile(InputCursor.isNumberChar).toString())
  }
  def readByte(): ByteNode = readNumber().toByteAst
  def readShort(): ShortNode = readNumber().toShortAst
  def readInt(): IntNode = readNumber().toIntAst
  def readLong(): LongNode = readNumber().toLongAst
  def readBigInt(): BigIntNode = readNumber().toBigIntAst
  def readFloat(): FloatNode = readNumber().toFloatAst
  def readDouble(): DoubleNode = readNumber().toDoubleAst
  def readBigDecimal(): BigDecimalNode = readNumber().toBigDecimalAst
  def skipWhiteSpace() {
    if (InputCursor.isWhitespace(iterator.current))
      while(iterator.hasNext && InputCursor.isWhitespace(iterator.next())) {  }
  }

  protected def readUnicode(totalChars: Int): Char = {
    var value: Int = 0
    var i: Int = 0
    while (i < totalChars) {
      value = value * 16
      i += 1
      val c = iterator.next()
      if (c <= '9' && c >= '0') value += c - '0'
      else if (c <= 'F' && c >= 'A') value += (c - 'A') + 10
      else if (c >= 'a' && c <= 'f') value += (c - 'a') + 10
      else if (!iterator.hasNext) throw failParse("Tried to move past the end of the input.")
      else throw failParse("Tried to move past the end of the input")
    }
    value.asInstanceOf[Char]
  }

  @tailrec final def nextNode(): AstNode = {
    if (iterator.hasNext) {
      (iterator.current: @switch) match {
        case ' ' | '\r' | '\n' | '\t' =>
          skipWhiteSpace()
          nextNode()
        case ':' | '}' | ']' => throw failParse(s"Unexpected char '${iterator.current}' at ${iterator.pos}")
        case '{' => readObject()
        case '[' => readArray()
        case '"' => readString() // double quotes for a string
        case '\'' => throw failParse(s"Unexpected char '${iterator.current}' at ${iterator.pos}") // TODO: Optional support for single quoted strings
        case 'n' =>
          if (size >= iterator.pos + 3 && iterator.peek == 'u' && iterator.charAt(iterator.pos + 2) == 'l' && iterator.charAt(iterator.pos + 3) == 'l') NullNode
          else throw failParse(s"Unexpected char '${iterator.current}' at ${iterator.pos}")
        case 't' | 'f' => readBoolean() // read a boolean
        case 'N' => throw failParse(s"Unexpected char '${iterator.current}' at ${iterator.pos}") // TODO: Optional NaN support
        case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' | '-' | '+' | 'e' | 'E' | '.' => readNumber() // read a number
        case _ => throw failParse(s"Unexpected char '${iterator.current}' at ${iterator.pos}")
      }
    } else throw failParse("Tried to move past the end of the input")
  } 
  
}

trait JsonInputFormat[R] extends InputFormat[R]

object Json extends JsonInputFormat[String] {
  type Cursor = JsonInputCursor[String]
  def createCursor(in: String): Cursor = new JsonStringCursor(in)
}

class JsonStringCursor(val source: String) extends JsonInputCursor[String] {

  protected val size = source.length
  protected def iterator: JsonSourceIterator = new JsonSourceIterator {
    // TODO: is direct memory better?
    // ByteBuffer.allocateDirect(source.length * 2).asCharBuffer().append(source) // allocate 3x the size because of UTF-8 encoding
    private[this] val b = CharBuffer.wrap(source)
    def charAt(idx: Int): Char = b.charAt(idx)
    def next(): Char = b.get
    def peek: Char = charAt(pos + 1)
    def hasNext: Boolean = b.hasRemaining
    def pos: Int = b.position
    def current: Char = charAt(pos)
  }
  
  
}