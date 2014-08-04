package muster
package codec

import _root_.play.api.libs.json._
import ast._
import output._

import scala.collection.mutable.ArrayBuffer


package object play {
  implicit object JsValueConsumer extends Consumer[JsValue] {
      def consume(node: AstNode[_]): JsValue = node match {
        case UndefinedNode => JsUndefined("")
        case NullNode => JsNull
        case n: BoolNode => JsBoolean(value = n.value)
        case ByteNode(b) => JsNumber(b)
        case ShortNode(s) => JsNumber(s)
        case IntNode(i) => JsNumber(i)
        case LongNode(l) => JsNumber(l)
        case BigIntNode(b) => JsNumber(BigDecimal(b))
        case FloatNode(i) => JsNumber(i)
        case DoubleNode(i) => JsNumber(i)
        case BigDecimalNode(b) => JsNumber(b)
        case n: NumberNode => JsNumber(n.toBigDecimal)
        case TextNode(s) => JsString(s)
        case node: ArrayNode =>
          val bldr = List.newBuilder[JsValue]
          while(node.hasNextNode) {
            bldr += consume(node.nextNode())
          }
          JsArray(bldr.result())
        case node: ObjectNode =>
          val bldr = ArrayBuffer.newBuilder[(String, JsValue)]
          val flds = node.keysIterator
          while(flds.hasNext) {
            val nm = flds.next()
            bldr += nm -> consume(node.readField(nm))
          }
          JsObject(bldr.result())
      }
    }
  
    implicit object JsValueProducer extends Producer[JsValue] {
      def produce(value: JsValue, formatter: OutputFormatter[_]): Unit = value match {
        case JsNull => formatter.writeNull()
        case _: JsUndefined => formatter.undefined()
        case JsString(s) => formatter.string(s)
        case JsNumber(i) => formatter.bigDecimal(i)
        case JsBoolean(true) => formatter.boolean(value = true)
        case JsBoolean(false) => formatter.boolean(value = false)
        case JsArray(values) =>
          formatter.startArray()
          values foreach (jv => produce(jv, formatter))
          formatter.endArray()
        case JsObject(fields) =>
          formatter.startObject()
          fields foreach { fld =>
            formatter.startField(fld._1)
            produce(fld._2, formatter)
          }
          formatter.endObject()
      }
    }
  
    implicit class JsValueConsumable(val value: JsValue) extends Consumable[JsValue]
}
