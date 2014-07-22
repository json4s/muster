package muster

import org.specs2.{ScalaCheck, Specification}
import muster.codec.jackson.api
import org.specs2.matcher.MatchResult
import org.scalacheck.{Gen, Prop}

class ProducerSpec extends Specification with ScalaCheck {

  val json = api.JsonFormat
  def write[T: Producer](obj: T) = json.from(obj)

  def cp[T](implicit toProp: MatchResult[T] => Prop, a: org.scalacheck.Arbitrary[T], s: org.scalacheck.Shrink[T], cons: Producer[T]) = {
    prop {
      (i: T) =>
        write(i) must_== i.toString
    }
  }

  def cpc[T, N](fn: T => N)(implicit toProp: MatchResult[N] => Prop, a: org.scalacheck.Arbitrary[T], s: org.scalacheck.Shrink[T], cons: Producer[N]) = {
    prop {
      (i: T) =>
        val conv = fn(i)
        write(conv) must_== i.toString
    }
  }

  def is =
s2"""
A Producer should
  write a byte value $byteProp
  write a short value $shortProp
  write a int value $intProp
  write a long value $longProp
  write a big int value $bigIntProp
  write a float value $floatProp
  write a double value $doubleProp
  write a big decimal value $bigDecimalProp
  write a java byte value $javaByteProp
  write a java short value $javaShortProp
  write a java int value $javaIntProp
  write a java long value $javaLongProp
  write a java big int value $javaBigIntProp
  write a java float value $javaFloatProp
  write a java double value $javaDoubleProp
  write a java big decimal value $javaBigDecimalProp
  write a string value $stringProp
  write a list value $listProp
  write a mutable list value $mutableListProp
  write a map with string keys value                      $mapProp
  write a map with string keys and a list value           $mapListProp
  write a map with int keys value                         $mapIntProp
  write a map with int keys and a list value              $mapIntListProp
  write a map with long keys value                        $mapLongProp
  write a map with long keys and a list value             $mapLongListProp
  write an option value                                   $optProp
  write an option with list value                         $optListProp
"""
  /*

   */

  val byteProp = cp[Byte]
  val shortProp = cp[Short]
  val intProp = cp[Int]
  val longProp = cp[Long]
  val bigIntProp = cp[BigInt]
  val floatProp = cp[Float]
  val doubleProp = cp[Double]
  val bigDecimalProp = cp[BigDecimal]
  val javaByteProp = cpc(byte2Byte(_: Byte))
  val javaShortProp = cpc(short2Short(_: Short))
  val javaIntProp = cpc(int2Integer(_: Int))
  val javaLongProp = cpc(long2Long(_: Long))
  val javaBigIntProp = cpc((_: BigInt).bigInteger)
  val javaFloatProp = cpc(float2Float(_: Float))
  val javaDoubleProp = cpc(double2Double(_: Double))
  val javaBigDecimalProp = cpc((_: BigDecimal).bigDecimal)
  val stringProp = prop {
    (i: String) =>
      val sb = new StringBuilder()
      sb.append('"')
      Quoter.jsonQuote(i, sb)
      sb.append('"')
      write(i) must_== sb.toString()
  }

  val listProp = prop {
    (i: List[Int]) => write(i) must_== i.mkString("[", ",", "]")
  }

  import collection.mutable

  val mutableListProp = prop {
     (i: mutable.ListBuffer[Int]) => write(i) must_== i.mkString("[", ",", "]")
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
            Quoter.jsonQuote(k, sb)
            sb.append('"')
            sb.append(':')
            sb.append(v)
        }
        sb.append('}')
        sb.toString
      }
      write(i) must_== json
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
            Quoter.jsonQuote(k, sb)
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
      write(i) must_== json
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
            Quoter.jsonQuote(k.toString, sb)
            sb.append('"')
            sb.append(':')
            sb.append(v)
        }
        sb.append('}')
        sb.toString
      }
      write(i) must_== json
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
            Quoter.jsonQuote(k.toString, sb)
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
      write(i) must_== json
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
            Quoter.jsonQuote(k.toString, sb)
            sb.append('"')
            sb.append(':')
            sb.append(v)
        }
        sb.append('}')
        sb.toString
      }
      write(i) must_== json
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
            Quoter.jsonQuote(k.toString, sb)
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
      write(i) must_== json
  }

  val optProp = prop {
    (i: Option[Int]) =>
      write(i) must_== i.fold("null")(_.toString)
  }

  val optListProp = prop {
    (i: List[Option[Int]]) =>
      write(i) must_== i.map(_.fold("null")(_.toString)).mkString("[", ",", "]")
  }


}