package muster

import org.specs2.matcher.MatchResult
import org.scalacheck.Prop

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
       Quoter.jsonQuote(x, sb)
       sb.append('"')
       fmt.result must_== sb.toString()
     }
   }


 }