package muster
package codec
package json

import java.util.TimeZone

import muster.util
import muster.util.Quoter
import org.scalacheck.{Gen, Prop}
import org.specs2.matcher.MatchResult
import org.specs2.{ScalaCheck, Specification}

abstract class JsonConsumerSpec(val json: InputFormat[Consumable[_], _ <: InputCursor[_]]) extends Specification with ScalaCheck {
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  //  def read[T](source: String)(implicit rdr: Readable[T]) = rdr.readFormatted(source, Muster.from.JsonString)
//  val json = JsonFormat

  def read[T: Consumer](js: String) = json.as[T](js)

  def cp[T](implicit toProp: MatchResult[T] => Prop, a: org.scalacheck.Arbitrary[T], s: org.scalacheck.Shrink[T], cons: Consumer[T]) = {
    prop {
      (i: T) =>
        json.as[T](i.toString) must_== i
    }
  }

  def cpc[T, N](fn: T => N)(implicit toProp: MatchResult[N] => Prop, a: org.scalacheck.Arbitrary[T], s: org.scalacheck.Shrink[T], cons: Consumer[N]) = {
    prop {
      (i: T) =>
        val conv = fn(i)
        read[N](conv.toString) must_== conv
    }
  }

    def is =
      s2"""
  A JSON Consumer should
    read a byte value                                      $byteProp
    read a short value                                     $shortProp
    read a int value                                       $intProp
    read a long value                                      $longProp
    read a big integer value                               $bigIntProp
    read a float value                                     $floatProp
    read a double value                                    $doubleProp
    read a big decimal value                               $bigDecimalProp
    read a java byte value                                 $javaByteProp
    read a java short value                                $javaShortProp
    read a java int value                                  $javaIntProp
    read a java long value                                 $javaLongProp
    read a java big integer value                          $javaBigIntProp
    read a java float value                                $javaFloatProp
    read a java double value                               $javaDoubleProp
    read a string value                                    $stringProp
    read a list value                                      $listProp
    read a list value                                      $mutableListProp
    read a map with string keys value                      $mapProp
    read a map with string keys and a list value           $mapListProp
    read a map with int keys value                         $mapIntProp
    read a map with int keys and a list value              $mapIntListProp
    read a map with long keys value                        $mapLongProp
    read a map with long keys and a list value             $mapLongListProp
    read an option value                                   $optProp
    read an option with list value                         $optListProp
  """
  //      "read a java big decimal value" ! javaBigDecimalProp ^ br ^
  //      "read a map with option int value, with null as default" ! optNullMapProp ^ br ^
  //      "read a map with option value, with missing as missing" ! optMissingMapProp ^ br ^


  val byteProp = cp[Byte]
  val shortProp = cp[Short]
  val intProp = cp[Int]
  val longProp = cp[Long]
  val bigIntProp = cp[BigInt]
  val floatProp = cp[Float]
  val doubleProp = cp[Double]
  val bigDecimalProp = cpc {
    (x: Double) => BigDecimal(x)
  }
  val javaByteProp = cpc {
    (x: Byte) => byte2Byte(x)
  }
  val javaShortProp = cpc {
    (x: Short) => short2Short(x)
  }
  val javaIntProp = cpc {
    (x: Int) => int2Integer(x)
  }
  val javaLongProp = cpc {
    (x: Long) => long2Long(x)
  }
  val javaBigIntProp = cpc {
    (x: BigInt) => x.bigInteger
  }
  val javaFloatProp = cpc {
    (x: Float) => float2Float(x)
  }
  val javaDoubleProp = cpc {
    (x: Double) => double2Double(x)
  }
  val javaBigDecimalProp = cpc {
    (x: BigDecimal) => x.bigDecimal
  }

  val stringProp = prop {
    (i: String) =>
      val sb = new StringBuilder()
      sb.append('"')
      Quoter.jsonQuote(i, util.Appendable.forString(sb))
      sb.append('"')
      read[String](sb.toString()) must_== i
  }

  val listProp = prop {
    (i: List[Int]) => read[List[Int]](i.mkString("[", ",", "]")) must_== i
  }

  import scala.collection.mutable

  val mutableListProp = prop {
    (i: mutable.ListBuffer[Int]) => read[mutable.ListBuffer[Int]](i.mkString("[", ",", "]")) must_== i
  }

  val mapGen = {
    for {
      n <- Gen.alphaStr.suchThat(s => s != null && s.trim.nonEmpty)
      m <- Gen.chooseNum(1, 999999999)
      t = (n, m)
      r <- Gen.mapOf(t)
    } yield r
  }

  val mapProp = Prop.forAll(mapGen) {
    (i: Map[String, Int]) =>
      val json = {
        val sb = new mutable.StringBuilder()
        sb.append('{')
        var first = true
        i foreach {
          case (k, v) =>
            if (!first) sb.append(',')
            else first = false
            sb.append('"')
            Quoter.jsonQuote(k, util.Appendable.forString(sb))
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
      n <- Gen.alphaStr.suchThat(s => s != null && s.trim.nonEmpty)
      m <- Gen.listOf(Gen.chooseNum(1, 999999999))
      t = (n, m)
      r <- Gen.mapOf(t)
    } yield r
  }

  val mapListProp = Prop.forAll(mapListGen) {
    (i: Map[String, List[Int]]) =>
      val json = {
        val sb = new mutable.StringBuilder()
        sb.append('{')
        var first = true
        i foreach {
          case (k, v) =>
            if (!first) sb.append(',')
            else first = false
            sb.append('"')
            Quoter.jsonQuote(k, util.Appendable.forString(sb))
            sb.append('"')
            sb.append(':')
            sb.append('[')
            var fe = true
            v foreach {
              e =>
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

  val intMapGen = {
    for {
      n <- Gen.posNum[Int]
      m <- Gen.chooseNum(1, 999999999)
      t = (n, m)
      r <- Gen.mapOf(t)
    } yield r
  }

  val mapIntProp = Prop.forAll(intMapGen) {
    (i: Map[Int, Int]) =>
      val json = {
        val sb = new mutable.StringBuilder()
        sb.append('{')
        var first = true
        i foreach {
          case (k, v) =>
            if (!first) sb.append(',')
            else first = false
            sb.append('"')
            Quoter.jsonQuote(k.toString, util.Appendable.forString(sb))
            sb.append('"')
            sb.append(':')
            sb.append(v)
        }
        sb.append('}')
        sb.toString
      }
      read[Map[Int, Int]](json) must_== i
  }

  val intMapListGen = {
    for {
      n <- Gen.posNum[Int]
      m <- Gen.listOf(Gen.chooseNum(1, 999999999))
      t = (n, m)
      r <- Gen.mapOf(t)
    } yield r
  }

  val mapIntListProp = Prop.forAll(intMapListGen) {
    (i: Map[Int, List[Int]]) =>
      val json = {
        val sb = new mutable.StringBuilder()
        sb.append('{')
        var first = true
        i foreach {
          case (k, v) =>
            if (!first) sb.append(',')
            else first = false
            sb.append('"')
            Quoter.jsonQuote(k.toString, util.Appendable.forString(sb))
            sb.append('"')
            sb.append(':')
            sb.append('[')
            var fe = true
            v foreach {
              e =>
                if (!fe) sb.append(',')
                else fe = false
                sb.append(e)
            }
            sb.append(']')
        }
        sb.append('}')
        sb.toString
      }
      read[Map[Int, List[Int]]](json) must_== i
  }

  val longMapGen = {
    for {
      n <- Gen.posNum[Long]
      m <- Gen.chooseNum(1, 999999999)
      t = (n, m)
      r <- Gen.mapOf(t)
    } yield r
  }

  val mapLongProp = Prop.forAll(longMapGen) {
    (i: Map[Long, Int]) =>
      val json = {
        val sb = new mutable.StringBuilder()
        sb.append('{')
        var first = true
        i foreach {
          case (k, v) =>
            if (!first) sb.append(',')
            else first = false
            sb.append('"')
            Quoter.jsonQuote(k.toString, util.Appendable.forString(sb))
            sb.append('"')
            sb.append(':')
            sb.append(v)
        }
        sb.append('}')
        sb.toString
      }
      read[Map[Long, Int]](json) must_== i
  }

  val longMapListGen = {
    for {
      n <- Gen.posNum[Long]
      m <- Gen.listOf(Gen.chooseNum(1, 999999999))
      t = (n, m)
      r <- Gen.mapOf(t)
    } yield r
  }

  val mapLongListProp = Prop.forAll(longMapListGen) {
    (i: Map[Long, List[Int]]) =>
      val json = {
        val sb = new mutable.StringBuilder()
        sb.append('{')
        var first = true
        i foreach {
          case (k, v) =>
            if (!first) sb.append(',')
            else first = false
            sb.append('"')
            Quoter.jsonQuote(k.toString, util.Appendable.forString(sb))
            sb.append('"')
            sb.append(':')
            sb.append('[')
            var fe = true
            v foreach {
              e =>
                if (!fe) sb.append(',')
                else fe = false
                sb.append(e)
            }
            sb.append(']')
        }
        sb.append('}')
        sb.toString
      }
      read[Map[Long, List[Int]]](json) must_== i
  }

  val optProp = prop {
    (i: Option[Int]) =>
      read[Option[Int]](i.map(_.toString).getOrElse("")) must_== i
  }

  val optListProp = prop {
    (i: List[Option[Int]]) =>
      read[List[Option[Int]]](i.map(_.map(_.toString).getOrElse("null")).mkString("[", ",", "]")) must_== i
  }

//
//  val mapOptionGen = {
//    for {
//      n <- Gen.alphaStr.suchThat(s => s != null && s.trim.nonEmpty)
//      m <- Gen.option(Gen.chooseNum(1, 999999999))
//      r <- Gen.mapOf((n, m))
//    } yield r
//  }
//  val optNullMapProp = Prop.forAll(mapOptionGen) {
//    (i: Map[String, Option[Int]]) =>
//      val json = {
//        val sb = new mutable.StringBuilder()
//        sb.append('{')
//        var first = true
//        i foreach {
//          case (k, v) =>
//            if (!first) sb.append(',')
//            else first = false
//            sb.append('"')
//            Quoter.jsonQuote(k, Appendable.forString(sb))
//            sb.append('"')
//            sb.append(':')
//            if (v.isDefined) sb.append(v)
//            else sb.append("null")
//        }
//        sb.append('}')
//        sb.toString
//      }
//      //println(s"The input: $i")
//      //println(s"The json: $json")
//      val r = read[Map[String, Option[Int]]](json)
//      //println(s"The result: $r")
//      r == i
//  }
//  val optMissingMapProp = Prop.forAll(mapOptionGen) {
//    (i: Map[String, Option[Int]]) =>
//      val json = {
//        val sb = new mutable.StringBuilder()
//        sb.append('{')
//        var first = true
//        i foreach {
//          case (k, v) =>
//            if (v.isDefined) {
//              if (!first) sb.append(',')
//              else first = false
//              sb.append('"')
//              Quoter.jsonQuote(k, Appendable.forString(sb))
//              sb.append('"')
//              sb.append(':')
//              sb.append(v)
//            }
//        }
//        sb.append('}')
//        sb.toString
//      }
//      read[Map[String, Option[Int]]](json) == i
//  }
}