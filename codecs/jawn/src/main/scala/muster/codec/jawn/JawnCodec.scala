package muster
package codec
package jawn

import java.nio.ByteBuffer
import java.nio.channels.Channels

import muster.ast._
import muster.codec.json.JsonRenderer
import muster.input.InputFormat

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

/** Provides a codec using the jawn library for dealing with json
  *
  * It combines a [[muster.Renderer]] with a [[InputFormat]] to provide
  * bidirectional encoding and decoding of JSON streams
  */
object JawnCodec extends JsonRenderer(StringProducible) with InputFormat[Consumable[_], JawnInputCursor] {
  implicit object jawnFacade extends _root_.jawn.MutableFacade[AstNode[_]] {
      def jarray(vs: ArrayBuffer[AstNode[_]]): AstNode[_] = new JawnInputCursor.JawnArrayNode(vs)
      def jobject(vs: mutable.Map[String, AstNode[_]]): AstNode[_] = new JawnInputCursor.JawnObjectNode(vs)
      def jint(s: String): AstNode[_] = NumberNode(s)
      def jfalse(): AstNode[_] = FalseNode
      def jnum(s: String): AstNode[_] = NumberNode(s)
      def jnull(): AstNode[_] = NullNode
      def jtrue(): AstNode[_] = TrueNode
      def jstring(s: String): AstNode[_] = TextNode(s)
    }
  def createCursor(in: Consumable[_]): JawnInputCursor = new JawnInputCursor {
    val source: AstNode[_] = parse(in)
  }

  private[this] def parse(in: Consumable[_]): AstNode[_] = {
    val p = _root_.jawn.Parser
    val tryResult = in match {
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
      case _root_.jawn.ParseException(msg, _, line, col) => throw new ParseException(msg, Some(ParseLocation(line, col)))
    }).get
  }
}