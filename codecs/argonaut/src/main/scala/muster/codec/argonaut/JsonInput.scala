package muster
package codec
package argonaut

import ast._
import muster.input.InputCursor
import _root_.argonaut._, Argonaut._

object ArgonautInputCursor {

  private final class JsonObjectNode(parent: Map[JsonField, Json]) extends ObjectNode(null) {

    def readField(fieldName: String): AstNode[_] = {
      parent.get(fieldName).fold(UndefinedNode.asInstanceOf[AstNode[_]]) { node => 
        node.fold(
          NullNode,
          b => if (b) TrueNode else FalseNode,
          n => DoubleNode(n),
          s => TextNode(s),
          arr => new JsonArrayNode(arr),
          obj => new JsonObjectNode(obj.toMap)
        )
      }
    }

    def readArrayFieldOpt(fieldName: String): Option[ArrayNode] = {
      parent.get(fieldName) flatMap { j =>
        j.fold(
          None,
          _ => throw new MappingException(s"Expected an array for $fieldName field but got a boolean"),
          _ => throw new MappingException(s"Expected an array for $fieldName field but got a number"),
          _ => throw new MappingException(s"Expected an array for $fieldName field but got a string"),
          arr => Some(new JsonArrayNode(arr)),
          _ => throw new MappingException(s"Expected an array for $fieldName field but got an object")
        )
      }
    }

    def readObjectFieldOpt(fieldName: String): Option[ObjectNode] = {
      parent.get(fieldName) flatMap { j =>
        j.fold(
          None,
          _ => throw new MappingException(s"Expected an object field for $fieldName but got a boolean"),
          _ => throw new MappingException(s"Expected an object field for $fieldName but got a number"),
          _ => throw new MappingException(s"Expected an object field for $fieldName but got a string"),
          _ => throw new MappingException(s"Expected an object field for $fieldName but got an array"),
          obj => Some(new JsonObjectNode(obj.toMap))
        )
      }
    }

    def readStringFieldOpt(fieldName: String): Option[TextNode] = {
      parent.get(fieldName) flatMap { j =>
        j.fold(
          None,
          _ => throw new MappingException(s"Expected a string field for $fieldName but got a boolean"),
          _ => throw new MappingException(s"Expected a string field for $fieldName but got a number"),
          s =>  Some(TextNode(s)),
          _ => throw new MappingException(s"Expected a string field for $fieldName but got an array"),
          _=> throw new MappingException(s"Expected a string field for $fieldName but got an object")
        )
      }
    }

    def readBooleanFieldOpt(fieldName: String): Option[BoolNode] = {
      parent.get(fieldName) flatMap { j =>
        j.fold(
          None,
          b => Some(if (b) TrueNode else FalseNode),
          _ => throw new MappingException(s"Expected a boolean field for $fieldName but got a number"),
          _ => throw new MappingException(s"Expected a boolean field for $fieldName but got a string"),
          _ => throw new MappingException(s"Expected a boolean field for $fieldName but got an array"),
          _=> throw new MappingException(s"Expected a boolean field for $fieldName but got an object")
        )
      }
    }

    def readNumberFieldOpt(fieldName: String): Option[NumberNode] = {
      parent.get(fieldName) flatMap { j =>
        j.fold(
          None,
          _ => throw new MappingException(s"Expected a number field for $fieldName but got a boolean"),
          n => Some(NumberNode(n.toString)),
          s => Some(NumberNode(s)),
          _ => throw new MappingException(s"Expected a number field for $fieldName but got an array"),
          _=> throw new MappingException(s"Expected a number field for $fieldName but got an object")
        )
      }
    }


    def keySet: Set[String] = parent.keySet

    def keysIterator: Iterator[String] = parent.keysIterator
  }

  private final class JsonArrayNode(val source: JsonArray) extends ArrayNode(null) with ArgonautInputCursor[JsonArray] {
    private[this] val iter = source.iterator
    protected def node = iter.next()
    override def hasNextNode: Boolean = iter.hasNext
  }

}
private[json4s] trait ArgonautInputCursor[R] extends InputCursor[R] {

  import ArgonautInputCursor._

  protected def node: Json

  def readArrayOpt(): Option[ArrayNode] = {
    node.fold(
      None,
      _ => throw new MappingException(s"Expected an array value but got a boolean"),
      _ => throw new MappingException(s"Expected an array value but got a number"),
      _ => throw new MappingException(s"Expected an array value but got a string"),
      arr => Some(new JsonArrayNode(arr)),
      _ => throw new MappingException(s"Expected an array value but got an object")
    )
  }

  def readObjectOpt(): Option[ObjectNode] = {
    this.node.fold(
      None,
      _ => throw new MappingException(s"Expected an object value but got a boolean"),
      _ => throw new MappingException(s"Expected an object value but got a number"),
      _ => throw new MappingException(s"Expected an object value but got a string"),
      _ => throw new MappingException(s"Expected an object value but got an array"),
      obj => Some(new JsonObjectNode(obj.toMap))
    )
  }

  def readStringOpt(): Option[TextNode] = {
    this.node.fold(
      None,
      _ => throw new MappingException(s"Expected a string value but got a boolean"),
      _ => throw new MappingException(s"Expected a string value but got a number"),
      s =>  Some(TextNode(s)),
      _ => throw new MappingException(s"Expected a string value but got an array"),
      _=> throw new MappingException(s"Expected a string value but got an object")
    )
  }

  def readBooleanOpt(): Option[BoolNode] = {
    this.node.fold(
      None,
      b => Some(if (b) TrueNode else FalseNode),
      _ => throw new MappingException(s"Expected a boolean value but got a number"),
      _ => throw new MappingException(s"Expected a boolean value but got a string"),
      _ => throw new MappingException(s"Expected a boolean value but got an array"),
      _=> throw new MappingException(s"Expected a boolean value but got an object")
    )
  }

  def readNumberOpt(): Option[NumberNode] = {
    this.node.fold(
      None,
      _ => throw new MappingException(s"Expected a number value but got a boolean"),
      n => Some(NumberNode(n.toString)),
      s => Some(NumberNode(s)),
      _ => throw new MappingException(s"Expected a number value but got an array"),
      _=> throw new MappingException(s"Expected a number value but got an object")
    )
  }

  def nextNode(): AstNode[_] = {    
    this.node.fold(
      NullNode,
      b => if (b) TrueNode else FalseNode,
      n => DoubleNode(n),
      s => TextNode(s),
      arr => new JsonArrayNode(arr),
      obj => new JsonObjectNode(obj.toMap)
    )
  }
}