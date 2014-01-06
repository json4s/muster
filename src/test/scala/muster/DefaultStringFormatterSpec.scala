package muster

import org.scalacheck._
import java.util.{TimeZone, Date}
import org.specs2.matcher.MatchResult

trait StringOutputFormatterSpec extends FormatterSpec[String] {

  def sharedFragments =
    "write a byte property" ! byteProp ^ br ^
    "write a short property" ! shortProp ^ br ^
    "write a int property" ! intProp ^ br ^
    "write a long property" ! longProp ^ br ^
    "write a big int property" ! bigIntProp ^ br ^
    "write a float property" ! floatProp ^ br ^
    "write a double property" ! doubleProp ^ br ^
    "write a big decimal property" ! bigDecimalProp ^ br ^
    "write a boolean property" ! boolProp ^ br ^
    "write a string property" ! stringProp ^ br ^
    "write a list property" ! listProp ^ br ^
    "write a object property" ! objectProp ^  br

  def writerProp[T](fn: (format.Formatter, T) => Unit)(implicit toProp : MatchResult[T] => Prop, a : org.scalacheck.Arbitrary[T], s : org.scalacheck.Shrink[T]) = prop { (x: T) =>
    withFormatter { fmt =>
      fn(fmt, x)
      fmt.result must_== x.toString
    }
  }

  def listProp: Prop
  def objectProp: Prop

  val byteProp = writerProp { (fmt, x:Byte) => fmt.byte(x) }
  val shortProp = writerProp { (fmt, x:Short) => fmt.short(x) }
  val intProp = writerProp { (fmt, x:Int) => fmt.int(x) }
  val longProp = writerProp { (fmt, x:Long) => fmt.long(x) }
  val bigIntProp = writerProp { (fmt, x:BigInt) => fmt.bigInt(x) }
  val floatProp = writerProp { (fmt, x:Float) => fmt.float(x) }
  val doubleProp = writerProp { (fmt, x:Double) => fmt.double(x) }
  val bigDecimalProp = writerProp { (fmt, x:BigDecimal) => fmt.bigDecimal(x) }
  val boolProp = writerProp { (fmt, x:Boolean) => fmt.boolean(x) }


  val stringProp = prop { (x: String) =>
    withFormatter { fmt =>
      fmt.string(x)
      val sb = new StringBuilder
      sb.append('"')
      JsonOutput.quote(x, sb)
      sb.append('"')
      fmt.result must_== sb.toString()
    }
  }


}

class DefaultStringFormatterSpec extends {val format = Muster.produce.String} with  StringOutputFormatterSpec {



  val listProp = writerProp { (fmt, lst: List[Int]) =>
    fmt.startArray("List")
    lst foreach fmt.int
    fmt.endArray()
  }

  val objectProp = prop { (obj: Category) =>
    withFormatter { fmt =>
      fmt.startObject("Category")
      fmt.startField("id")
      fmt.int(obj.id)
      fmt.startField("name")
      fmt.string(obj.name)
      fmt.endObject()
      fmt.result must_== s"""Category(id: ${obj.id}, name: "${obj.name}")"""
    }
  }

  val is = "The default string formatter should" ^ br ^ sharedFragments ^ end

}

