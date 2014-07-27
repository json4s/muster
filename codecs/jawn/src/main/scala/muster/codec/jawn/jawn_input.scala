package muster
package codec
package jawn

import muster.ast._
import muster.input.InputCursor

import scala.collection.mutable

private[jawn] object JawnInputCursor {

  private[jawn] final class JawnArrayNode(array: mutable.ArrayBuffer[AstNode[_]]) extends ArrayNode(null) with JawnInputCursor {

    val iter = array.iterator

    def source: AstNode[_] = iter.next()

    override def hasNextNode: Boolean = iter.hasNext
  }

  private[jawn] final class JawnObjectNode(values: mutable.Map[String, AstNode[_]]) extends ObjectNode(null) {
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
private[jawn] trait JawnInputCursor extends InputCursor[AstNode[_]] {

  def readStringOpt(): Option[TextNode] = {
    source match {
      case NullNode | UndefinedNode => None
      case node: TextNode => Some(node)
      case node => throw new MappingException(s"Expected a string but found a ${node.getClass.getSimpleName}")
    }

  }

  def readBooleanOpt(): Option[BoolNode] = {
    source match {
      case NullNode | UndefinedNode => None
      case node: BoolNode => Some(node)
      case node => throw new MappingException(s"Expected a boolean but found a ${node.getClass.getSimpleName}")
    }

  }

  def readArrayOpt(): Option[ArrayNode] = {
    source match {
      case NullNode | UndefinedNode => None
      case node: ArrayNode => Some(node)
      case node => throw new MappingException(s"Expected an array but found a ${node.getClass.getSimpleName}")
    }
  }

  def readNumberOpt(): Option[NumberNode] = {
    source match {
      case NullNode | UndefinedNode => None
      case node: NumberNode => Some(node)
      case node: TextNode => Some(NumberNode(node.value))
      case node => throw new MappingException(s"Expected a number but found a ${node.getClass.getSimpleName}")
    }

  }

  def readObjectOpt(): Option[ObjectNode] = {
    source match {
      case NullNode | UndefinedNode => None
      case node: ObjectNode => Some(node)
      case node => throw new MappingException(s"Expected an object but found a ${node.getClass.getSimpleName}")
    }

  }

  def nextNode(): AstNode[_] = source
}


