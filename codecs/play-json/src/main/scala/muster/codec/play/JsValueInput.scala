package muster
package codec
package play

import ast._
import input._
import _root_.play.api.libs.json._

object Json4sInputCursor {

  private final class Json4sObjectNode(parent: JsObject) extends ObjectNode(null) {
    private[this] val fields: Map[String, JsValue] = parent.value.toMap

    def readField(fieldName: String): AstNode[_] = {
      readFieldFromParent(fieldName) match {
        case JsNull => NullNode
        case _: JsUndefined => UndefinedNode
        case JsString(s) => TextNode(s)
        case JsNumber(d) => BigDecimalNode(d)
        case JsBoolean(true) => TrueNode
        case JsBoolean(false) => FalseNode
        case node: JsArray => new JsArrayNode(node)
        case node: JsObject => new Json4sObjectNode(node)
      }
    }

    private[this] def readFieldFromParent(name: String): JsValue =
      fields.getOrElse(name, JsUndefined(s"Couldn't find field for '$name'"))

    def readArrayFieldOpt(fieldName: String): Option[ArrayNode] = {
      readFieldFromParent(fieldName) match {
        case JsNull | _: JsUndefined => None
        case node: JsArray => Some(new JsArrayNode(node))
        case node => throw new MappingException(s"Expected an array field but found a ${node.getClass.getSimpleName}")
      }
    }

    def readObjectFieldOpt(fieldName: String): Option[ObjectNode] = {
      readFieldFromParent(fieldName) match {
        case JsNull | _: JsUndefined => None
        case node: JsObject => Some(new Json4sObjectNode(node))
        case node => throw new MappingException(s"Expected an object field but found a ${node.getClass.getSimpleName}")
      }
    }

    def readStringFieldOpt(fieldName: String): Option[TextNode] = {
      readFieldFromParent(fieldName) match {
        case JsNull | _: JsUndefined => None
        case JsString(s) => Some(TextNode(s))
        case node => throw new MappingException(s"Expected a string field but found a ${node.getClass.getSimpleName}")
      }
    }

    def readBooleanFieldOpt(fieldName: String): Option[BoolNode] = {
      readFieldFromParent(fieldName) match {
        case JsNull | _: JsUndefined => None
        case JsBoolean(true) => Some(TrueNode)
        case JsBoolean(false) => Some(FalseNode)
        case node => throw new MappingException(s"Expected a boolean field but found a ${node.getClass.getSimpleName}")
      }
    }

    def readNumberFieldOpt(fieldName: String): Option[NumberNode] = {
      readFieldFromParent(fieldName) match {
        case JsNull | _: JsUndefined => None
        case JsNumber(i) => Some(NumberNode(i.toString()))
        case JsString(s) => Some(NumberNode(s))
        case node => throw new MappingException(s"Expected a number field but found a ${node.getClass.getSimpleName}")
      }
    }


    def keySet: Set[String] = fields.keySet.toSet

    def keysIterator: Iterator[String] = fields.keysIterator
  }

  private final class JsArrayNode(val source: JsArray) extends ArrayNode(null) with Json4sInputCursor[JsArray] {
    private[this] val iter = source.value.iterator
    protected def node = iter.next()
    override def hasNextNode: Boolean = iter.hasNext
  }

}
private[play] trait Json4sInputCursor[R] extends InputCursor[R] {

  import Json4sInputCursor._

  protected def node: JsValue

  def readArrayOpt(): Option[ArrayNode] = {
    this.node match {
      case JsNull | _: JsUndefined => None
      case node: JsArray => Some(new JsArrayNode(node))
      case node => throw new MappingException(s"Expected an array value but found a ${node.getClass.getSimpleName}")
    }
  }

  def readObjectOpt(): Option[ObjectNode] = {
    this.node match {
      case JsNull | _: JsUndefined => None
      case node: JsObject => Some(new Json4sObjectNode(node))
      case node => throw new MappingException(s"Expected an object value but found a ${node.getClass.getSimpleName}")
    }
  }

  def readStringOpt(): Option[TextNode] = {
    this.node match {
      case JsNull | _: JsUndefined => None
      case JsString(s) => Some(new TextNode(s))
      case node => throw new MappingException(s"Expected a string value but found a ${node.getClass.getSimpleName}")
    }
  }

  def readBooleanOpt(): Option[BoolNode] = {
    this.node match {
      case JsNull | _: JsUndefined => None
      case JsBoolean(true) => Some(TrueNode)
      case JsBoolean(false) => Some(FalseNode)
      case node => throw new MappingException(s"Expected a boolean value but found a ${node.getClass.getSimpleName}")
    }
  }

  def readNumberOpt(): Option[NumberNode] = {
    this.node match {
      case JsNull | _: JsUndefined => None
      case JsNumber(i) => Some(NumberNode(i.toString()))
      case JsString(s) => Some(NumberNode(s))
      case node => throw new MappingException(s"Expected a number field but found a ${node.getClass.getSimpleName}")
    }
  }

  def nextNode(): AstNode[_] = {
    this.node match {
      case JsNull => NullNode
      case _: JsUndefined => UndefinedNode
      case JsString(s) => TextNode(s)
      case JsNumber(d) => BigDecimalNode(d)
      case JsBoolean(true) => TrueNode
      case JsBoolean(false) => FalseNode
      case node: JsArray => new JsArrayNode(node)
      case node: JsObject => new Json4sObjectNode(node)
    }
  }
}