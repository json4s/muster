package muster
package codec

import muster.ast._
import muster.output.OutputFormatter
import org.json4s.JsonAST._

import scala.language.implicitConversions

package object json4s {

  implicit object JValueConsumer extends Consumer[JValue] {
    def consume(node: AstNode[_]): JValue = node match {
      case UndefinedNode => JNothing
      case NullNode => JNull
      case n: BoolNode => JBool(value = n.value)
      case ByteNode(b) => JInt(b)
      case ShortNode(s) => JInt(s)
      case IntNode(i) => JInt(i)
      case LongNode(l) => JInt(l)
      case BigIntNode(b) => JInt(b)
      case FloatNode(i) => JDecimal(i)
      case DoubleNode(i) => JDecimal(i)
      case BigDecimalNode(b) => JDecimal(b)
      case n: NumberNode => if (n.value.contains(".")) JDecimal(n.toBigDecimal) else JInt(n.toBigInt)
      case TextNode(s) => JString(s)
      case node: ArrayNode =>
        val bldr = List.newBuilder[JValue]
        while(node.hasNextNode) {
          bldr += consume(node.nextNode())
        }
        JArray(bldr.result())
      case node: ObjectNode =>
        val bldr = List.newBuilder[JField]
        val flds = node.keysIterator
        while(flds.hasNext) {
          val nm = flds.next()
          bldr += nm -> consume(node.readField(nm))
        }
        JObject(bldr.result())
    }
  }

  implicit object JValueProducer extends Producer[JValue] {
    def produce(value: JValue, formatter: OutputFormatter[_]): Unit = value match {
      case JNull => formatter.writeNull()
      case JNothing => formatter.undefined()
      case JString(s) => formatter.string(s)
      case JInt(i) => formatter.bigInt(i)
      case JDecimal(d) => formatter.bigDecimal(d)
      case JDouble(d) => formatter.double(d)
      case JBool(v) => formatter.boolean(value = v)
      case JArray(values) =>
        formatter.startArray()
        values foreach (jv => produce(jv, formatter))
        formatter.endArray()
      case JObject(fields) =>
        formatter.startObject()
        fields foreach { fld =>
          formatter.startField(fld._1)
          produce(fld._2, formatter)
        }
        formatter.endObject()
    }
  }

  implicit class JvalueConsumable(val value: JValue) extends Consumable[JValue]
}
