---
layout: default
---
## Integrating a parser

For integrating your parser into muster you need to provide an implementation of an input cursor.  The steps below explains how the jawn parser was integrated. 

In the case of jawn you need to provide a facade from jawn to the muster AST so that jawn parses directly to the AST node we expect.
This facade functions as our entry point into the muster AST. 

```scala
implicit object jawnFacade extends _root_.jawn.MutableFacade[AstNode[_]] {

  def jarray(vs: ArrayBuffer[AstNode[_]]): AstNode[_] = new JawnArrayNode(vs)

  def jobject(vs: mutable.Map[String, AstNode[_]]): AstNode[_] = new JawnObjectNode(vs)

  def jint(s: String): AstNode[_] = Ast.NumberNode(s)

  def jfalse(): AstNode[_] = Ast.FalseNode

  def jnum(s: String): AstNode[_] = Ast.NumberNode(s)

  def jnull(): AstNode[_] = Ast.NullNode

  def jtrue(): AstNode[_] = Ast.TrueNode

  def jstring(s: String): AstNode[_] = Ast.TextNode(s)
}
```

This facade will be used in the `JawnInputCursor` where you use the jawn parser to parse to the consumables into an AST cursor. 

```scala
class JawnInputCursor(val source: Consumable[_]) extends JawnInputCursorBase {
  def parsed: AstNode[_] = {
    val p = _root_.jawn.Parser
    source match {
      case StringConsumable(src) => p.parseFromString(src).getOrElse(UndefinedNode)
      case FileConsumable(src) => p.parseFromFile(src).getOrElse(UndefinedNode)
      case InputStreamConsumable(src) => p.parseFromChannel(Channels.newChannel(src)).getOrElse(UndefinedNode)
      case ByteArrayConsumable(src) => p.parseFromByteBuffer(ByteBuffer.wrap(src)).getOrElse(UndefinedNode)
      case URLConsumable(src) => {
        val strm = src.openConnection().getInputStream
        try {
          p.parseFromChannel(Channels.newChannel(strm)).getOrElse(UndefinedNode)
        } finally {
          strm.close()
        }
      }
    }
  }
}
```

This `JawnInputCursor` is an implementation of `JawnInputCursorBase`. Each of the cursors provides a method that returns an untyped muster AST node. 

```scala
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
```

The `JawnInputCursor` exists because its implementation is shared between the `JawnInputCursor` and the `JawnArrayNode`
A `JawnArrayNode` iterates over all the nodes in the provided array and provides 

```scala
class JawnArrayNode(array: mutable.ArrayBuffer[AstNode[_]]) extends ArrayNode(null) with JawnInputCursorBase {
  def source: Consumable[_] = null

  val iter = array.iterator

  def parsed: AstNode[_] = iter.next()

  override def hasNextNode: Boolean = iter.hasNext
}
```

You still need to provide an implementation of an `AstCursor` for reading objects. This is the `muster.Ast.ObjectNode`

```scala
class JawnObjectNode(values: mutable.Map[String, AstNode[_]]) extends ObjectNode(null) {
  
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
```

This class turns a json object from the stream into an ast. It starts by extending `muster.ObjectNode(null)` and not passing it a parent cursor in this case. Objects from Jawn in the way we use jawn are greedy and so we have the materialized AST already by implementing the parsing facade from earlier.
