package muster
package input

import muster.ast._

import scala.util.Try





/** Groups all the error generation methods together */
trait CursorFailures {
  protected def failStructure(msg: String) = throw new MappingException(msg)
}

/** AstCursor provides a cursor over the muster AST
  *
  */
trait AstCursor extends CursorFailures {
  def readArray(): ArrayNode = readArrayOpt().getOrElse(notFound(classOf[ArrayNode]))

  def readObject(): ObjectNode = readObjectOpt().getOrElse(notFound(classOf[ObjectNode]))

  def readString(): TextNode = readStringOpt().getOrElse(notFound(classOf[TextNode]))

  def readBoolean(): BoolNode = readBooleanOpt().getOrElse(notFound(classOf[BoolNode]))

  def readNumber(): NumberNode = readNumberOpt().getOrElse(notFound(classOf[NumberNode]))

  def readByte(): ByteNode = readByteOpt().getOrElse(notFound(classOf[ByteNode]))

  def readShort(): ShortNode = readShortOpt().getOrElse(notFound(classOf[ShortNode]))

  def readInt(): IntNode = readIntOpt().getOrElse(notFound(classOf[IntNode]))

  def readLong(): LongNode = readLongOpt().getOrElse(notFound(classOf[LongNode]))

  def readBigInt(): BigIntNode = readBigIntOpt().getOrElse(notFound(classOf[BigIntNode]))

  def readFloat(): FloatNode = readFloatOpt().getOrElse(notFound(classOf[FloatNode]))

  def readDouble(): DoubleNode = readDoubleOpt().getOrElse(notFound(classOf[DoubleNode]))

  def readBigDecimal(): BigDecimalNode = readBigDecimalOpt().getOrElse(notFound(classOf[BigDecimalNode]))

  def readArrayOpt(): Option[ArrayNode]

  def readObjectOpt(): Option[ObjectNode]

  def readStringOpt(): Option[TextNode]

  def readBooleanOpt(): Option[BoolNode]

  def readNumberOpt(): Option[NumberNode]

  def readByteOpt(): Option[ByteNode] = readNumberOpt().map(_.toByteAst)

  def readShortOpt(): Option[ShortNode] = readNumberOpt().map(_.toShortAst)

  def readIntOpt(): Option[IntNode] = readNumberOpt().map(_.toIntAst)

  def readLongOpt(): Option[LongNode] = readNumberOpt().map(_.toLongAst)

  def readBigIntOpt(): Option[BigIntNode] = readNumberOpt().map(_.toBigIntAst)

  def readFloatOpt(): Option[FloatNode] = readNumberOpt().map(_.toFloatAst)

  def readDoubleOpt(): Option[DoubleNode] = readNumberOpt().map(_.toDoubleAst)

  def readBigDecimalOpt(): Option[BigDecimalNode] = readNumberOpt().map(_.toBigDecimalAst)

  def hasNextNode: Boolean = false

  def nextNode(): AstNode[_]
  private[this] def notFound(node: Class[_]) = failStructure(s"Could not find element of ${node.getSimpleName}")
}

trait InputCursor[R] extends AstCursor {
  def source: R
}

trait InputFormat[R, C <: InputCursor[_]] {
  def createCursor(in: R): C

  def as[T](source: R)(implicit consumer: Consumer[T]): T = {
    try {
      val cursor = createCursor(source)
      consumer.consume(cursor.nextNode())
    } catch {
      case t: Throwable => consumer.recover.applyOrElse(t, (i: Throwable) => throw i)
    }
  }

  def tryAs[T](source: R)(implicit consumer: Consumer[T]): Try[T] = Try(as(source))

}