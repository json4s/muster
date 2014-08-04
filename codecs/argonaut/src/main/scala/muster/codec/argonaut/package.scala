package muster
package codec

import _root_.argonaut._, Argonaut._
import output.OutputFormatter
import ast._

import scala.collection.mutable.ArrayBuffer

package object argonaut {

  implicit object JValueConsumer extends Consumer[Json] {
    def consume(node: AstNode[_]): Json = node match {
      case UndefinedNode => jNull
      case NullNode => jNull
      case n: BoolNode => jBool(n.value)
      case ByteNode(b) => jNumber(b)
      case ShortNode(s) => jNumber(s)
      case IntNode(i) => jNumber(i)
      case LongNode(l) => jNumber(l)
      case BigIntNode(b) => jNumber(b.toDouble)
      case FloatNode(i) => jNumber(i)
      case DoubleNode(i) => jNumber(i)
      case BigDecimalNode(b) => jNumber(b.toDouble)
      case n: NumberNode => jNumber(n.toDouble)
      case TextNode(s) => jString(s)
      case node: ArrayNode =>
        val bldr = List.newBuilder[Json]
        while (node.hasNextNode) {
          bldr += consume(node.nextNode())
        }
        JArray(bldr.result())
      case node: ObjectNode =>
        val bldr = ArrayBuffer.empty[(Json.JsonField, Json)]
        val flds = node.keysIterator
        while (flds.hasNext) {
          val nm = flds.next()
          bldr += nm -> consume(node.readField(nm))
        }
        Json.obj(bldr:_*)
    }
  }

  implicit object JsonProducer extends Producer[Json] {
      def produce(value: Json, formatter: OutputFormatter[_]): Unit = {
         value.fold(
          formatter.writeNull(),
          b => formatter.boolean(b),
          n => formatter.double(n),
          s => formatter.string(s),
          arr => {
            formatter.startArray()
            arr foreach { j =>
              produce(j, formatter)
            }
            formatter.endArray()
          },
          obj => {
            formatter.startObject()
            obj.toList foreach { kv =>
              formatter.startField(kv._1)
              produce(kv._2, formatter)
            }
            formatter.endObject()
          }
        )
      }
    }
  implicit class JsonConsumable(val value: Json) extends Consumable[Json]
}
