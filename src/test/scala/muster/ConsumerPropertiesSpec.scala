package muster

import org.scalacheck._
import java.util.TimeZone
import scala.reflect.ClassTag
import muster.StringOutputFormatter._
import org.specs2.{ScalaCheck, Specification}
import org.specs2.matcher.MatchResult
//import org.joda.time.DateTime

class ConsumerSpec extends Specification with ScalaCheck {
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  //  def read[T](source: String)(implicit rdr: Readable[T]) = rdr.readFormatted(source, Muster.from.JsonString)
  val format = Muster.consume.Json

  def read[T: Consumer](js: String) = format.as[T](js)

  def cp[T](implicit toProp : MatchResult[T] => Prop, a : org.scalacheck.Arbitrary[T], s : org.scalacheck.Shrink[T], cons: Consumer[T]) = {
    prop { (i: T) =>
      format.as[T](i.toString) must_== i
    }
  }

  def cpc[T, N](fn: T => N)(implicit toProp : MatchResult[N] => Prop, a : org.scalacheck.Arbitrary[T], s : org.scalacheck.Shrink[T], cons: Consumer[N]) = {
    prop { (i: T) =>
      val conv = fn(i)
      read[N](conv.toString) must_== conv
    }
  }

  def is =
    "A consumer should" ^
      "read a byte value" ! byteProp ^ br ^
      "read a short value" ! shortProp ^ br ^
      "read a int value" ! intProp ^ br ^
      "read a long value" ! longProp ^ br ^
      "read a big integer value" ! bigIntProp ^ br ^
      "read a float value" ! floatProp ^ br ^
      "read a double value" ! doubleProp ^ br ^
      "read a big decimal value" ! bigDecimalProp ^ br ^
      "read a java byte value" ! javaByteProp ^ br ^
      "read a java short value" ! javaShortProp ^ br ^
      "read a java int value" ! javaIntProp ^ br ^
      "read a java long value" ! javaLongProp ^ br ^
      "read a java big integer value" ! javaBigIntProp ^ br ^
      "read a java float value" ! javaFloatProp ^ br ^
      "read a java double value" ! javaDoubleProp ^ br ^
//      "read a java big decimal value" ! javaBigDecimalProp ^ br ^
      "read a string value" ! stringProp ^ br ^
      "read a list value" ! listProp ^ br ^
      "read a map value" ! mapProp ^ br ^
      "read a map with list value" ! mapListProp ^ br ^
      "read an option value" ! optProp ^ br ^
      "read an option with list value" ! optListProp ^ br ^
      "read an option with option int value, with null as default" ! optNullMapProp ^ br ^
      "read an option with option value, with missing as missing" ! optMissingMapProp ^ br ^
    end



  val byteProp = cp[Byte]
  val shortProp = cp[Short]
  val intProp = cp[Int]
  val longProp = cp[Long]
  val bigIntProp = cp[BigInt]
  val floatProp = cp[Float]
  val doubleProp = cp[Double]
  val bigDecimalProp = cpc { (x: Double) => BigDecimal(x) }
  val javaByteProp = cpc { (x: Byte) => byte2Byte(x) }
  val javaShortProp = cpc { (x: Short) => short2Short(x) }
  val javaIntProp = cpc { (x: Int) => int2Integer(x) }
  val javaLongProp = cpc { (x: Long) => long2Long(x) }
  val javaBigIntProp = cpc { (x: BigInt) => x.bigInteger }
  val javaFloatProp = cpc { (x: Float) => float2Float(x) }
  val javaDoubleProp = cpc { (x: Double) => double2Double(x) }
  val javaBigDecimalProp = cpc { (x: BigDecimal) => x.bigDecimal }

  val stringProp = prop { (i: String) =>
    val sb = new StringBuilder()
    sb.append('"')
    JsonOutput.quote(i, sb)
    sb.append('"')
    read[String](sb.toString()) must_== i
  }

  val listProp = prop { (i: List[Int]) => read[List[Int]](i.mkString("[", ",", "]")) must_== i  }
  import collection.mutable
  val mutableListProp = prop { (i: mutable.ListBuffer[Int]) => read[mutable.ListBuffer[Int]](i.mkString("[", ",", "]")) must_== i  }

  val mapGen = {
    for {
      n <- Gen.alphaStr
      m <- Gen.chooseNum(1, 999999999)
      t = (n, m)
      r <- Gen.mapOf(t)
    } yield r
  }

  val mapProp = Prop.forAll(mapGen) { (i: Map[String, Int]) =>
    val json = {
      val sb = new mutable.StringBuilder()
      sb.append('{')
      var first = true
      i foreach { case (k, v) =>
        if (!first) sb.append(',')
        else first = false
        sb.append('"')
        JsonOutput.quote(k, sb)
        sb.append('"')
        sb.append(':')
        sb.append(v)
      }
      sb.append('}')
      sb.toString
    }
    read[Map[String, Int]](json) must_== i
  }

  val mapListGen = {
    for {
      n <- Gen.alphaStr
      m <- Gen.listOf(Gen.chooseNum(1, 999999999))
      t = (n, m)
      r <- Gen.mapOf(t)
    } yield r
  }

  val mapListProp = Prop.forAll(mapListGen) { (i: Map[String, List[Int]]) =>
    val json = {
      val sb = new mutable.StringBuilder()
      sb.append('{')
      var first = true
      i foreach { case (k, v) =>
        if (!first) sb.append(',')
        else first = false
        sb.append('"')
        JsonOutput.quote(k, sb)
        sb.append('"')
        sb.append(':')
        sb.append('[')
        var fe = true
        v foreach { e =>
          if (!fe) sb.append(',')
          else fe = false
          sb.append(e)
        }
        sb.append(']')
      }
      sb.append('}')
      sb.toString
    }
    read[Map[String, List[Int]]](json) must_== i
  }

  val optProp = prop { (i: Option[Int]) =>
    read[Option[Int]](i.map(_.toString).getOrElse("")) must_== i
  }

  val optListProp = prop { (i: List[Option[Int]]) =>
    read[List[Option[Int]]](i.map(_.map(_.toString).getOrElse("null")).mkString("[", ",", "]")) must_== i
  }



   val mapOptionGen = {
     for {
       n <- Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString).suchThat(_.forall(_.isLetter))
       m <- Gen.option(Gen.chooseNum(1, 999999999))
       r <- Gen.mapOf((n, m))
     } yield  r
   }
   val optNullMapProp = Prop.forAll(mapOptionGen) { (i: Map[String, Option[Int]]) =>
      val json = {
        val sb = new mutable.StringBuilder()
        sb.append('{')
        var first = true
        i foreach { case (k, v) =>
          if (!first) sb.append(',')
          else first = false
          sb.append('"')
          JsonOutput.quote(k, sb)
          sb.append('"')
          sb.append(':')
          if (v.isDefined) sb.append(v)
          else sb.append("null")
        }
        sb.append('}')
        sb.toString
      }
      read[Map[String, Option[Int]]](json) == i
   }
   val optMissingMapProp = Prop.forAll(mapOptionGen) { (i: Map[String, Option[Int]]) =>
      val json = {
        val sb = new mutable.StringBuilder()
        sb.append('{')
        var first = true
        i foreach { case (k, v) =>
          if (v.isDefined) {
            if (!first) sb.append(',')
            else first = false
            sb.append('"')
            JsonOutput.quote(k, sb)
            sb.append('"')
            sb.append(':')
            sb.append(v)
          }
        }
        sb.append('}')
        sb.toString
      }
      read[Map[String, Option[Int]]](json) == i
   }
}
