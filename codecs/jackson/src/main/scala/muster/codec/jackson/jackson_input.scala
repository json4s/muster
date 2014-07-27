package muster
package codec
package jackson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ArrayNode => JArrayNode}
import muster.ast._

import scala.collection.JavaConverters._


private object JacksonInputCursor {

  private final class JacksonObjectNode(parent: JsonNode) extends ObjectNode(null) {

    def readField(fieldName: String): AstNode[_] = {
      val node = readFieldFromParent(fieldName)
      if (node.isNull) NullNode
      else if (node.isMissingNode) UndefinedNode
      else if (node.isArray) new JacksonArrayNode(node)
      else if (node.isObject) new JacksonObjectNode(node)
      else if (node.isTextual) TextNode(node.asText())
      else if (node.isNumber) NumberNode(node.asText())
      else if (node.isBoolean) {
        if (node.asBoolean()) TrueNode else FalseNode
      } else throw new MappingException("Unable to determine the type of this json")
    }

    private[this] def readFieldFromParent(name: String): JsonNode = if (parent.has(name)) parent.get(name) else com.fasterxml.jackson.databind.node.NullNode.getInstance()

    def readArrayFieldOpt(fieldName: String): Option[ArrayNode] = {
      val node = readFieldFromParent(fieldName)
      if (node.isNull || node.isMissingNode) None
      else if (node.isArray) Some(new JacksonArrayNode(node))
      else throw new MappingException(s"Expected an array field but found a ${node.getClass.getSimpleName}")
    }

    def readObjectFieldOpt(fieldName: String): Option[ObjectNode] = {
      val node = readFieldFromParent(fieldName)
      if (node.isNull || node.isMissingNode) None
      else if (node.isObject) Some(new JacksonObjectNode(node))
      else throw new MappingException(s"Expected an object field but found a ${node.getClass.getSimpleName}")
    }

    def readStringFieldOpt(fieldName: String): Option[TextNode] = {
      val node = readFieldFromParent(fieldName)
      if (node.isNull || node.isMissingNode) None
      else if (node.isTextual) Some(TextNode(node.asText()))
      else throw new MappingException(s"Expected a string field but found a ${node.getClass.getSimpleName}")
    }

    def readBooleanFieldOpt(fieldName: String): Option[BoolNode] = {
      val node = readFieldFromParent(fieldName)
      if (node.isNull || node.isMissingNode) None
      else if (node.isBoolean) Some(if (node.asBoolean()) TrueNode else FalseNode)
      else throw new MappingException(s"Expected a boolean field but found a ${node.getClass.getSimpleName}")
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

  private final class JacksonArrayNode(val source: JsonNode) extends ArrayNode(null) with JacksonInputCursor[JsonNode] {
    private[this] var idx = 0

    protected def node = {
      val c = source.get(idx)
      idx += 1
      c
    }

    override def hasNextNode: Boolean = source.asInstanceOf[JArrayNode].size() > idx
  }
}

private[jackson] trait JacksonInputCursor[R] extends InputCursor[R] {

  import muster.codec.jackson.JacksonInputCursor._

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
    else throw new MappingException(s"Expected a string but found a ${node.getClass.getSimpleName}")
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
    else if (node.isTextual) TextNode(node.asText())
    else if (node.isNumber) NumberNode(node.asText())
    else if (node.isBoolean) {
      if (node.asBoolean()) TrueNode else FalseNode
    } else throw new MappingException("Unable to determine the type of this json")
  }
}

