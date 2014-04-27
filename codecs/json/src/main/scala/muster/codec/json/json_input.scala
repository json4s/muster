package muster
package codec
package json

import Ast._
import java.nio.CharBuffer
import scala.annotation.{tailrec, switch}
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, JsonNode}
import com.fasterxml.jackson.databind.node.{ ArrayNode => JArrayNode }
import scala.collection.JavaConverters._
import java.text.DateFormat

trait JsonReader[R] extends Iterator[Char] {
  def source: R

  def pos: Int

  def charAt(idx: Int): Char

  def current: Char

  def peek: Char

  def remaining: Int
}

object CharBufferJsonReader {
  def apply(source: String) = new CharBufferJsonReader(source)
}

final class CharBufferJsonReader(val source: String) extends JsonReader[String] {
  val internal: CharBuffer = CharBuffer.wrap(source)
  private[this] var curr: Char = internal.get()
  private[this] var idx = internal.position()
  private[this] val size = source.length

  def pos: Int = idx - 1

  def charAt(index: Int): Char = internal.charAt(index - idx)

  def current: Char = curr

  def peek: Char = charAt(idx)

  def remaining: Int = size - pos

  def hasNext: Boolean = remaining > 0

  def next(): Char = {
    if (internal.hasRemaining) curr = internal.get()
    if (pos != internal.position()) idx += 1
    curr
  }
}

object StringJsonReader {
  def apply(source: String) = new StringJsonReader(source)
}

final class StringJsonReader(val source: String) extends JsonReader[String] {
  private[this] var curr: Char = source.charAt(0)
  private[this] var idx = 1
  private[this] val size = source.length

  def remaining = size - pos

  def charAt(idx: Int): Char = source.charAt(idx)

  def next(): Char = {
    if (size - idx > 0) curr = charAt(idx)
    if (pos != size) idx += 1
    curr
  }

  def peek: Char = charAt(idx)

  def hasNext: Boolean = remaining > 0

  def pos: Int = idx - 1

  def current: Char = curr

}

object JsonInputCursor {
  private[this] val notNextField = (c: Char) => {
    InputCursor.isWhitespace(c) || c == ','
  }
  private[this] val notFieldValue = (c: Char) => {
    InputCursor.isWhitespace(c) || c == ':'
  }

  import scala.language.existentials

  final case class JsonArrayNode(cursor: JsonInputCursor[_]) extends ArrayNode(cursor) with AstCursor {
    private[this] val eleName = "element"

    private[this] def iterator = cursor.iterator

    private[this] def skipToNextValue = {
      while (notNextField(iterator.current)) {
        iterator.next()
      }
    }

    private[this] var fields = {
      val buf = Vector.newBuilder[AstNode[_]]
      if (iterator.hasNext) iterator.next()
      skipToNextValue
      while (iterator.current != ']' && iterator.hasNext) {
        buf += cursor.nextNode()
        skipToNextValue
      }
      if (iterator.current != ']') failParse(s"Unexpected character when looking for ']' at ${iterator.pos}")
      if (iterator.hasNext) iterator.next()
      buf.result()
    }

    def nextNode(): AstNode[_] = if (hasNextNode) {
      val hd = fields.head
      fields = fields.tail
      hd
    } else failStructure("No more elements in the JsonArrayNode!")

    override def hasNextNode = !fields.isEmpty

    private[this] def withNext[T <: AstNode[_]](klass: Class[T]): Option[T] = {
      nextNode() match {
        case NullNode | UndefinedNode => None
        case nxt =>
          if (klass.isAssignableFrom(nxt.getClass)) Some(nxt.asInstanceOf[T])
          else err(nxt, klass.getSimpleName)
      }
    }

    private[this] def err(x: AstNode[_], nodeType: String) =
      failStructure(s"Expected to have an $nodeType $eleName but got ${x.getClass.getSimpleName}")

    def readArrayOpt(): Option[ArrayNode] = withNext(classOf[JsonArrayNode])

    def readObjectOpt(): Option[ObjectNode] = withNext(classOf[JsonObjectNode])

    def readStringOpt(): Option[TextNode] = withNext(classOf[TextNode])

    def readBooleanOpt(): Option[BoolNode] = withNext(classOf[BoolNode])

    def readNumberOpt(): Option[NumberNode] = withNext(classOf[NumberNode])

    override def toString: String = s"JsonArrayNode($fields)"
  }


  final case class JsonObjectNode(cursor: JsonInputCursor[_]) extends ObjectNode(cursor) {
    private[this] val eleName = "field"

    private[this] def iterator = cursor.iterator

    private[this] def skipToNextField = {
      while (notNextField(iterator.current)) {
        iterator.next()
      }
    }

    private[this] def skipToFieldValue = {
      if (iterator.hasNext && !notNextField(iterator.current)) iterator.next()
      while (notFieldValue(iterator.current)) {
        iterator.next()
      }
    }

    private[this] val fields = {
      val buf = new java.util.HashMap[String, AstNode[_]]
      if (iterator.hasNext) iterator.next() // move past '{'
      else failParse(s"Unexpected character ${iterator.current} at ${iterator.pos}.")
      skipToNextField
      while (iterator.current != '}' && iterator.hasNext) {
        val field = cursor.readString()
        skipToFieldValue
        buf.put(field.value.toString, cursor.nextNode())
        skipToNextField

      }
      if (iterator.current != '}') failParse(s"Unexpected character or end of input when looking for '}' at ${iterator.pos}")
      if (iterator.hasNext) iterator.next()
      buf
    }

    def keySet = fields.keySet.asScala.toSet

    def keysIterator = fields.keySet().asScala.toIterator

    private[this] def err(x: AstNode[_], fieldName: String, nodeType: Class[_]) =
      failStructure(s"Expected to have an ${nodeType.getSimpleName} $eleName for '$fieldName' but got ${x.getClass.getSimpleName}")

    private[this] def readFieldFromParent[T](fieldName: String, nodeType: Class[T]): Option[T] = {
      val fldOpt = fields.get(fieldName)
      if (fldOpt != null) {
        val x = fldOpt
        if (nodeType.isAssignableFrom(x.getClass)) Some(x.asInstanceOf[T])
        else if (nodeType.getSimpleName.startsWith("NullNode")) None
        else err(x, fieldName, nodeType)
      } else None
    }

    def readArrayFieldOpt(fieldName: String): Option[ArrayNode] = readFieldFromParent(fieldName, classOf[JsonArrayNode])

    def readObjectFieldOpt(fieldName: String): Option[ObjectNode] = readFieldFromParent(fieldName, classOf[JsonObjectNode])

    def readStringFieldOpt(fieldName: String): Option[TextNode] = readFieldFromParent(fieldName, classOf[TextNode])

    def readBooleanFieldOpt(fieldName: String): Option[BoolNode] = readFieldFromParent(fieldName, classOf[BoolNode])

    def readNumberFieldOpt(fieldName: String): Option[NumberNode] = readFieldFromParent(fieldName, classOf[NumberNode])

    def readField(fieldName: String): AstNode[_] = {
      readFieldFromParent(fieldName, classOf[AstNode[_]]).getOrElse(NullNode)
    }


    override def toString: String = s"JsonObjectNode($fields)"
  }

}

trait JsonInputCursor[R] extends InputCursor[R] {

  import JsonInputCursor._

  protected def size: Int

  protected def iterator: JsonReader[R]

  def readArrayOpt(): Option[ArrayNode] = Some(JsonArrayNode(this))

  def readObjectOpt(): Option[ObjectNode] = Some(JsonObjectNode(this))

  def readStringOpt(): Option[TextNode] = {
    skipWhiteSpace()
    val end = iterator.current // store quote as separator
    var shouldContinue = iterator.hasNext
    var seenEnd = false
    val sb = new java.lang.StringBuilder
    while (shouldContinue) {
      (iterator.next(): @switch) match {
        case '"' | '\'' =>
          if (end == iterator.current) {
            if (iterator.hasNext) iterator.next()
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
              case 'u' =>
                val chars = Array(iterator.next(), iterator.next(), iterator.next(), iterator.next())
                val codePoint = Integer.parseInt(new String(chars), 16)
                sb.appendCodePoint(codePoint)
              case 'x' =>
                val chars = Array(iterator.next(), iterator.next())
                val codePoint = Integer.parseInt(new String(chars), 16)
                sb.appendCodePoint(codePoint)
              case _ =>
            }
            shouldContinue = iterator.hasNext
          }
        case '\u0000' | 1 | 2 | 3 | 4 | 5 | 6 | 7 | '\b' | '\t' | '\n' | 11 | '\f' | '\r' | 14 | 15 |
             16 | 17 | 18 | 19 | 20 | 21 | 22 | 23 | 24 | 25 | 27 | 28 | 29 | 30 | 31 | 127 =>
          throw failParse(s"Unexpected control char at ${iterator.pos}")
        case c =>
          sb.append(c)
          shouldContinue = iterator.hasNext
      }

    }
    if (!seenEnd) throw failParse(s"Input ended too early while trying to find a closing quote for a string at ${iterator.pos}")
    Some(TextNode(sb.toString()))
  }

  def readBooleanOpt(): Option[BoolNode] = {
    skipWhiteSpace()
    val node = if (iterator.current == 't' &&
      iterator.peek == 'r' &&
      iterator.charAt(iterator.pos + 2) == 'u' &&
      iterator.charAt(iterator.pos + 3) == 'e') {
      iterator.next()
      iterator.next()
      iterator.next()
      TrueNode
    } else if (iterator.current == 'f' &&
      iterator.peek == 'a' &&
      iterator.charAt(iterator.pos + 2) == 'l' &&
      iterator.charAt(iterator.pos + 3) == 's' &&
      iterator.charAt(iterator.pos + 4) == 'e') {
      iterator.next()
      iterator.next()
      iterator.next()
      iterator.next()
      FalseNode
    } else throw failParse(s"Unexpected char '${iterator.current}' at ${iterator.pos}")

    if (iterator.hasNext && iterator.current == 'e') iterator.next()
    Some(node)
  }

  def readNumberOpt(): Option[NumberNode] = {
    skipWhiteSpace()
    val sb = new StringBuilder()
    while (iterator.hasNext && InputCursor.isNumberChar(iterator.current)) {
      sb.append(iterator.current)
      iterator.next()
    }
    Some(NumberNode(sb.toString()))
  }

  def skipWhiteSpace() {
    while (iterator.hasNext && InputCursor.isWhitespace(iterator.current)) {
      iterator.next()
    }
  }

  override def hasNextNode = iterator.hasNext

  @tailrec final def nextNode(): AstNode[_] = {
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

trait JsonInputFormat[R] extends InputFormat[R, JsonInputCursor[_]] {
}

object MusterJson extends JsonInputFormat[String] {
  def createCursor(in: String): JsonInputCursor[_] = new JsonStringCursor(CharBufferJsonReader(in))

  def withDateFormat(df: DateFormat): JsonInputFormat[String] = new JsonInputFormat[String] {
    def createCursor(in: String): JsonInputCursor[_] = new JsonStringCursor(CharBufferJsonReader(in))
  }
}

class JsonStringCursor(val iterator: JsonReader[String]) extends JsonInputCursor[String] {

  def source: String = iterator.source

  protected val size = source.length


}

object JackonInputCursor {

  final class JacksonObjectNode(parent: JsonNode) extends ObjectNode(null) {

    def readField(fieldName: String): AstNode[_] = {
      val node = readFieldFromParent(fieldName)
      if (node.isNull) NullNode
      else if (node.isMissingNode) UndefinedNode
      else if (node.isArray) new JacksonArrayNode(node)
      else if (node.isObject) new JacksonObjectNode(node)
      else if (node.isTextual) Ast.TextNode(node.asText())
      else if (node.isNumber) Ast.NumberNode(node.asText())
      else if (node.isBoolean) {
        if (node.asBoolean()) TrueNode else FalseNode
      } else throw new MappingException("Unable to determine the type of this json")
    }

    private[this] def readFieldFromParent(name: String): JsonNode = if (parent.has(name)) parent.get(name) else com.fasterxml.jackson.databind.node.NullNode.getInstance()

    def readArrayFieldOpt(fieldName: String): Option[ArrayNode] = {
      val node = readFieldFromParent(fieldName)
      if (node.isNull || node.isMissingNode) None
      else if (node.isArray) Some(new JacksonArrayNode(node))
      else throw new MappingException(s"Expected an array but found a ${node.getClass.getSimpleName}")
    }

    def readObjectFieldOpt(fieldName: String): Option[ObjectNode] = {
      val node = readFieldFromParent(fieldName)
      if (node.isNull || node.isMissingNode) None
      else if (node.isObject) Some(new JacksonObjectNode(node))
      else throw new MappingException(s"Expected an object but found a ${node.getClass.getSimpleName}")
    }

    def readStringFieldOpt(fieldName: String): Option[TextNode] = {
      val node = readFieldFromParent(fieldName)
      if (node.isNull || node.isMissingNode) None
      else if (node.isTextual) Some(TextNode(node.asText()))
      else throw new MappingException(s"Expected an array but found a ${node.getClass.getSimpleName}")
    }

    def readBooleanFieldOpt(fieldName: String): Option[BoolNode] = {
      val node = readFieldFromParent(fieldName)
      if (node.isNull || node.isMissingNode) None
      else if (node.isBoolean) Some(if (node.asBoolean()) TrueNode else FalseNode)
      else throw new MappingException(s"Expected an array but found a ${node.getClass.getSimpleName}")
    }

    def readNumberFieldOpt(fieldName: String): Option[NumberNode] = {
      val node = readFieldFromParent(fieldName)
      if (node.isNull || node.isMissingNode) None
      else if (node.isTextual) Some(NumberNode(node.asText()))
      else if (node.isNumber) Some(NumberNode(node.asText()))
      else throw new MappingException(s"Expected a number field but found a ${node.getClass.getSimpleName}")
    }


    def keySet: Set[String] = parent.fieldNames().asScala.toSet

    def keysIterator: Iterator[String] = parent.fieldNames().asScala
  }

  final class JacksonArrayNode(val source: JsonNode) extends ArrayNode(null) with JacksonInputCursor[JsonNode] {
    private[this] var idx = 0

    protected def node = {
      val c = source.get(idx)
      idx += 1
      c
    }

    override def hasNextNode: Boolean = source.asInstanceOf[JArrayNode].size() > idx
  }

}

trait JacksonInputCursor[R] extends InputCursor[R] {

  import JackonInputCursor._

  protected def node: JsonNode

  def readArrayOpt(): Option[ArrayNode] = {
    val node = this.node
    if (node.isNull || node.isMissingNode) None
    else if (node.isArray) Some(new JacksonArrayNode(node))
    else throw new MappingException(s"Expected an array but found a ${node.getClass.getSimpleName}")
  }

  def readObjectOpt(): Option[ObjectNode] = {
    val node = this.node
    if (node.isNull || node.isMissingNode) None
    else if (node.isObject) Some(new JacksonObjectNode(node))
    else throw new MappingException(s"Expected an object but found a ${node.getClass.getSimpleName}")
  }

  def readStringOpt(): Option[TextNode] = {
    val node = this.node
    if (node.isNull || node.isMissingNode) None
    else if (node.isTextual) Some(TextNode(node.asText()))
    else throw new MappingException(s"Expected an array but found a ${node.getClass.getSimpleName}")
  }

  def readBooleanOpt(): Option[BoolNode] = {
    val node = this.node
    if (node.isNull || node.isMissingNode) None
    else if (node.isBoolean) {
      Some(if (node.asBoolean()) TrueNode else FalseNode)
    }
    else throw new MappingException(s"Expected a boolean but found a ${node.getClass.getSimpleName}")
  }

  def readNumberOpt(): Option[NumberNode] = {
    val node = this.node
    if (node.isNull || node.isMissingNode) None
    else if (node.isNumber) Some(NumberNode(node.asText()))
    else if (node.isTextual) Some(NumberNode(node.asText()))
    else throw new MappingException(s"Expected a number but found a ${node.getClass.getSimpleName}")
  }

  def nextNode(): AstNode[_] = {
    val node = this.node
    if (node.isNull) NullNode
    else if (node.isMissingNode) UndefinedNode
    else if (node.isArray) new JacksonArrayNode(node)
    else if (node.isObject) new JacksonObjectNode(node)
    else if (node.isTextual) Ast.TextNode(node.asText())
    else if (node.isNumber) Ast.NumberNode(node.asText())
    else if (node.isBoolean) {
      if (node.asBoolean()) TrueNode else FalseNode
    } else throw new MappingException("Unable to determine the type of this json")
  }
}


trait JacksonInputFormat[R] extends InputFormat[R, JacksonInputCursor[_]] {
  val mapper: ObjectMapper = new ObjectMapper()
  mapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)

  def typeHintFieldName: String = "$typeName"


}

