package muster
package codec
package json4s

import Ast._
import org.json4s.JsonAST._

object Json4sInputCursor {

  private final class Json4sObjectNode(parent: JObject) extends ObjectNode(null) {
    private[this] val fields: Map[String, JValue] = parent.obj.toMap

    def readField(fieldName: String): AstNode[_] = {
      readFieldFromParent(fieldName) match {
        case JNull => NullNode
        case JNothing => UndefinedNode
        case JString(s) => TextNode(s)
        case JInt(i) => BigIntNode(i)
        case JDecimal(d) => BigDecimalNode(d)
        case JDouble(d) => DoubleNode(d)
        case JBool(true) => TrueNode
        case JBool(false) => FalseNode
        case node: JArray => new Json4sArrayNode(node)
        case node: JObject => new Json4sObjectNode(node)
      }
    }

    private[this] def readFieldFromParent(name: String): JValue =
      fields.getOrElse(name, JNothing)

    def readArrayFieldOpt(fieldName: String): Option[ArrayNode] = {
      readFieldFromParent(fieldName) match {
        case JNull | JNothing => None
        case node: JArray => Some(new Json4sArrayNode(node))
        case node => throw new MappingException(s"Expected an array field but found a ${node.getClass.getSimpleName}")
      }
    }

    def readObjectFieldOpt(fieldName: String): Option[ObjectNode] = {
      readFieldFromParent(fieldName) match {
        case JNull | JNothing => None
        case node: JObject => Some(new Json4sObjectNode(node))
        case node => throw new MappingException(s"Expected an object field but found a ${node.getClass.getSimpleName}")
      }
    }

    def readStringFieldOpt(fieldName: String): Option[TextNode] = {
      readFieldFromParent(fieldName) match {
        case JNull | JNothing => None
        case JString(s) => Some(TextNode(s))
        case node => throw new MappingException(s"Expected a string field but found a ${node.getClass.getSimpleName}")
      }
    }

    def readBooleanFieldOpt(fieldName: String): Option[BoolNode] = {
      readFieldFromParent(fieldName) match {
        case JNull | JNothing => None
        case JBool(true) => Some(TrueNode)
        case JBool(false) => Some(FalseNode)
        case node => throw new MappingException(s"Expected a boolean field but found a ${node.getClass.getSimpleName}")
      }
    }

    def readNumberFieldOpt(fieldName: String): Option[NumberNode] = {
      readFieldFromParent(fieldName) match {
        case JNull | JNothing => None
        case JInt(i) => Some(NumberNode(i.toString()))
        case JDecimal(d) => Some(NumberNode(d.toString()))
        case JDouble(d) => Some(NumberNode(d.toString()))
        case JString(s) => Some(NumberNode(s))
        case node => throw new MappingException(s"Expected a number field but found a ${node.getClass.getSimpleName}")
      }
    }


    def keySet: Set[String] = fields.keySet.toSet

    def keysIterator: Iterator[String] = fields.keysIterator
  }

  private final class Json4sArrayNode(val source: JArray) extends ArrayNode(null) with Json4sInputCursor[JArray] {
    private[this] val iter = source.arr.iterator
    protected def node = iter.next()
    override def hasNextNode: Boolean = iter.hasNext
  }

}

class EntryJson4sInputCursor(val source: JValue) extends Json4sInputCursor[JValue] {
  protected def node: JValue = source
}
sealed trait Json4sInputCursor[R] extends InputCursor[R] {

  import Json4sInputCursor._

  protected def node: JValue

  def readArrayOpt(): Option[ArrayNode] = {
    this.node match {
      case JNull | JNothing => None
      case node: JArray => Some(new Json4sArrayNode(node))
      case node => throw new MappingException(s"Expected an array value but found a ${node.getClass.getSimpleName}")
    }
  }

  def readObjectOpt(): Option[ObjectNode] = {
    this.node match {
      case JNull | JNothing => None
      case node: JObject => Some(new Json4sObjectNode(node))
      case node => throw new MappingException(s"Expected an object value but found a ${node.getClass.getSimpleName}")
    }
  }

  def readStringOpt(): Option[TextNode] = {
    this.node match {
      case JNull | JNothing => None
      case JString(s) => Some(new TextNode(s))
      case node => throw new MappingException(s"Expected a string value but found a ${node.getClass.getSimpleName}")
    }
  }

  def readBooleanOpt(): Option[BoolNode] = {
    this.node match {
      case JNull | JNothing => None
      case JBool(true) => Some(TrueNode)
      case JBool(false) => Some(FalseNode)
      case node => throw new MappingException(s"Expected a boolean value but found a ${node.getClass.getSimpleName}")
    }
  }

  def readNumberOpt(): Option[NumberNode] = {
    this.node match {
      case JNull | JNothing => None
      case JInt(i) => Some(NumberNode(i.toString()))
      case JDecimal(d) => Some(NumberNode(d.toString()))
      case JDouble(d) => Some(NumberNode(d.toString))
      case JString(s) => Some(NumberNode(s))
      case node => throw new MappingException(s"Expected a number field but found a ${node.getClass.getSimpleName}")
    }
  }

  def nextNode(): AstNode[_] = {
    this.node match {
      case JNull => NullNode
      case JNothing => UndefinedNode
      case JString(s) => TextNode(s)
      case JInt(i) => BigIntNode(i)
      case JDecimal(d) => BigDecimalNode(d)
      case JDouble(d) => DoubleNode(d)
      case JBool(true) => TrueNode
      case JBool(false) => FalseNode
      case node: JArray => new Json4sArrayNode(node)
      case node: JObject => new Json4sObjectNode(node)
    }
  }
}

case class JValueConsumable(value: JValue) extends Consumable[JValue]



