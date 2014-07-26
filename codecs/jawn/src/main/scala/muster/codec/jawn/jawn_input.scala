package muster
package codec
package jawn

import ast._
import java.nio.ByteBuffer
import java.nio.channels.Channels

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

object JawnInputCursor {

  private final class JawnArrayNode(array: mutable.ArrayBuffer[AstNode[_]]) extends ArrayNode(null) with JawnInputCursorBase {
    def source: Consumable[_] = null

    val iter = array.iterator

    def parsed: AstNode[_] = iter.next()

    override def hasNextNode: Boolean = iter.hasNext
  }

  private final class JawnObjectNode(values: mutable.Map[String, AstNode[_]]) extends ObjectNode(null) {
    def readArrayFieldOpt(fieldName: String): Option[ArrayNode] = {
      values get fieldName flatMap {
        case NullNode | UndefinedNode => None
        case node: ArrayNode => Some(node)
        case node => throw new MappingException(s"Expected an array field but found a ${node.getClass.getSimpleName}")
      }
    }

    def readObjectFieldOpt(fieldName: String): Option[ObjectNode] = {
      values.get(fieldName) flatMap {
        case NullNode | UndefinedNode => None
        case node: ObjectNode => Some(node)
        case node => throw new MappingException(s"Expected an object field but found a ${node.getClass.getSimpleName}")
      }
    }

    def readField(fieldName: String): AstNode[_] =
      values.getOrElse(fieldName, throw new MappingException("Unable to determine the type of this json"))

    def readNumberFieldOpt(fieldName: String): Option[NumberNode] = {
      values get fieldName flatMap {
        case NullNode | UndefinedNode => None
        case node: NumberNode => Some(node)
        case node: TextNode => Some(NumberNode(node.value))
        case node => throw new MappingException(s"Expected a number field but found a ${node.getClass.getSimpleName}")
      }
    }

    def readStringFieldOpt(fieldName: String): Option[TextNode] = {
      values get fieldName flatMap {
        case NullNode | UndefinedNode => None
        case node: TextNode => Some(node)
        case node => throw new MappingException(s"Expected a string field but found a ${node.getClass.getSimpleName}")
      }
    }

    def readBooleanFieldOpt(fieldName: String): Option[BoolNode] = {
      values get fieldName flatMap {
        case NullNode | UndefinedNode => None
        case m: BoolNode => Some(m)
        case m => throw new MappingException(s"Expected a boolean field but found a ${m.getClass.getSimpleName}")
      }
    }

    def keysIterator: Iterator[String] = values.keysIterator

    def keySet: Set[String] = values.keySet.toSet
  }

}
sealed trait JawnInputCursorBase extends InputCursor[Consumable[_]] {


  def parsed: AstNode[_]

  def readStringOpt(): Option[TextNode] = {
    parsed match {
      case NullNode | UndefinedNode => None
      case node: TextNode => Some(node)
      case node => throw new MappingException(s"Expected a string but found a ${node.getClass.getSimpleName}")
    }

  }

  def readBooleanOpt(): Option[BoolNode] = {
    parsed match {
      case NullNode | UndefinedNode => None
      case node: BoolNode => Some(node)
      case node => throw new MappingException(s"Expected a boolean but found a ${node.getClass.getSimpleName}")
    }

  }

  def readArrayOpt(): Option[ArrayNode] = {
    parsed match {
      case NullNode | UndefinedNode => None
      case node: ArrayNode => Some(node)
      case node => throw new MappingException(s"Expected an array but found a ${node.getClass.getSimpleName}")
    }
  }

  def readNumberOpt(): Option[NumberNode] = {
    parsed match {
      case NullNode | UndefinedNode => None
      case node: NumberNode => Some(node)
      case node: TextNode => Some(NumberNode(node.value))
      case node => throw new MappingException(s"Expected a number but found a ${node.getClass.getSimpleName}")
    }

  }

  def readObjectOpt(): Option[ObjectNode] = {
    parsed match {
      case NullNode | UndefinedNode => None
      case node: ObjectNode => Some(node)
      case node => throw new MappingException(s"Expected an object but found a ${node.getClass.getSimpleName}")
    }

  }

  def nextNode(): AstNode[_] = parsed
}

class JawnInputCursor(val source: Consumable[_]) extends JawnInputCursorBase {
  import JawnInputCursor._
  implicit object jawnFacade extends _root_.jawn.MutableFacade[AstNode[_]] {
    def jarray(vs: ArrayBuffer[AstNode[_]]): AstNode[_] = new JawnArrayNode(vs)
    def jobject(vs: mutable.Map[String, AstNode[_]]): AstNode[_] = new JawnObjectNode(vs)
    def jint(s: String): AstNode[_] = NumberNode(s)
    def jfalse(): AstNode[_] = FalseNode
    def jnum(s: String): AstNode[_] = NumberNode(s)
    def jnull(): AstNode[_] = NullNode
    def jtrue(): AstNode[_] = TrueNode
    def jstring(s: String): AstNode[_] = TextNode(s)
  }

  def parsed: AstNode[_] = {
    val p = _root_.jawn.Parser
    val tryResult = source match {
      case StringConsumable(src) => p.parseFromString(src)
      case FileConsumable(src) => p.parseFromFile(src)
      case InputStreamConsumable(src) => p.parseFromChannel(Channels.newChannel(src))
      case ByteArrayConsumable(src) => p.parseFromByteBuffer(ByteBuffer.wrap(src))
      case URLConsumable(src) =>
        for {
          stream <- Try(Channels.newChannel(src.openConnection().getInputStream))
          parsed <- p.parseFromChannel(stream)
          _ <- Try(stream.close())
        } yield parsed
      case ByteChannelConsumable(src) => p.parseFromChannel(src)
      case ByteBufferConsumable(src) => p.parseFromByteBuffer(src)
      case ReaderConsumable(src) => p.parseFromChannel(Consumable.readerChannel(src))
    }
    (tryResult recover {
      case t: _root_.jawn.IncompleteParseException => throw new EndOfInput
    }).get
  }
}

