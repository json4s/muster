package muster

import org.scalacheck._
import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.ISODateTimeFormat
import java.util.Date
import scala.reflect.ClassTag
import muster.StringOutputFormatter._

object ReadableSpec extends Properties("Readable") {
  import Prop.forAll

  DateTimeZone.setDefault(DateTimeZone.UTC)

  def read[T](source: String)(implicit rdr: Readable[T]) = rdr.readFormated(source, Muster.from.JsonString)


  def cp[T:Arbitrary:Readable:ClassTag] = {
    property(implicitly[ClassTag[T]].runtimeClass.getSimpleName + " value") = forAll { (i: T) =>
      read[T](i.toString) == i
    }
  }

  cp[Byte]
  cp[Short]
  cp[Int]
  cp[Long]
  cp[BigInt]
  cp[Float]
  cp[Double]

  // The artbitrary for big decimal generates numbers it can't parse back in
  property("BigDecimal value") = forAll { (i: Double) =>
    val bd = BigDecimal(i)
    read[BigDecimal](bd.toString) == bd
  }
  property("java.lang.Byte value") = forAll { (i: Byte) =>
    val bd = byte2Byte(i)
    read[java.lang.Byte](bd.toString) == bd
  }
  property("java.lang.Byte value") = forAll { (i: Short) =>
    val bd = short2Short(i)
    read[java.lang.Short](bd.toString) == bd
  }
  property("java.lang.Integer value") = forAll { (i: Int) =>
    val bd = int2Integer(i)
    read[Integer](bd.toString) == bd
  }
  property("java.lang.Long value") = forAll { (i: Long) =>
    val bd = long2Long(i)
    read[java.lang.Long](bd.toString) == bd
  }
  property("java.math.BigInteger value") = forAll { (i: BigInt) =>
    val bd = i.bigInteger
    read[java.math.BigInteger](bd.toString) == bd
  }
  property("java.lang.Float value") = forAll { (i: Float) =>
    val bd = float2Float(i)
    read[java.lang.Float](bd.toString) == bd
  }
  property("java.lang.Double value") = forAll { (i: Double) =>
    val bd = double2Double(i)
    read[java.lang.Double](bd.toString) == bd
  }
//  property("java.math.BigDecimal value") = forAll { (i: Double) =>
//    val bd = BigDecimal(i.toString)
//    read[java.math.BigDecimal](i.toString) == bd.bigDecimal
//  }

  property("String value") = forAll(Gen.alphaStr) { (i: String) =>
    val sb = new StringBuilder()
    sb.append('"')
    quote(i, sb)
    sb.append('"')
    read[String](sb.toString()) == i
  }

  property("List value") = forAll { (i: List[Int]) =>
    read[List[Int]](i.mkString("[",",","]")) == i
  }

  import collection.mutable
  property("mutable List value") = forAll { (i: mutable.ListBuffer[Int]) =>
    read[mutable.ListBuffer[Int]](i.mkString("[",",","]")) == i
  }

  val mapGen = {
    for {
      n <- Gen.alphaStr
      m <- Gen.chooseNum(1, 999999999)
      t = (n, m)
      r <- Gen.mapOf(t)
    } yield  r
  }
  property("map value") = forAll(mapGen) { (i: Map[String, Int]) =>
    val json = {
      val sb = new mutable.StringBuilder()
      sb.append('{')
      var first = true
      i foreach { case (k, v) =>
        if (!first) sb.append(',')
        else first = false
        sb.append('"')
        quote(k, sb)
        sb.append('"')
        sb.append(':')
        sb.append(v)
      }
      sb.append('}')
      sb.toString
    }
    read[Map[String, Int]](json) == i
  }

  val mapListGen = {
    for {
      n <- Gen.alphaStr
      m <- Gen.listOf(Gen.chooseNum(1, 999999999))
      t = (n, m)
      r <- Gen.mapOf(t)
    } yield  r
  }
  property("map with list value") = forAll(mapListGen) { (i: Map[String, List[Int]]) =>
    val json = {
      val sb = new mutable.StringBuilder()
      sb.append('{')
      var first = true
      i foreach { case (k, v) =>
        if (!first) sb.append(',')
        else first = false
        sb.append('"')
        quote(k, sb)
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
    read[Map[String, List[Int]]](json) == i
  }

  property("option value") = forAll { (i: Option[Int]) =>
    read[Option[Int]](i.map(_.toString).getOrElse("")) == i
  }

  property("option value in  a list" ) = forAll { (i: List[Option[Int]]) =>
    read[List[Option[Int]]](i.map(_.map(_.toString).getOrElse("null")).mkString("[", ",", "]")) == i
  }

//  val mapOptionGen = {
//   for {
//     n <- Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString).suchThat(_.forall(_.isLetter))
//     m <- Gen.option(Gen.chooseNum(1, 999999999))
//     r <- Gen.mapOf((n, m))
//   } yield  r
// }
// property("option value as null in a map") = forAll(mapOptionGen) { (i: Map[String, Option[Int]]) =>
//   val json = {
//     val sb = new mutable.StringBuilder()
//     sb.append('{')
//     var first = true
//     i foreach { case (k, v) =>
//       if (!first) sb.append(',')
//       else first = false
//       sb.append('"')
//       quote(k, sb)
//       sb.append('"')
//       sb.append(':')
//       if (v.isDefined) sb.append(v)
//       else sb.append("null")
//     }
//     sb.append('}')
//     sb.toString
//   }
//   read[Map[String, Option[Int]]](json) == i
// }
// property("option value as missing in a map") = forAll(mapOptionGen) { (i: Map[String, Option[Int]]) =>
//   val json = {
//     val sb = new mutable.StringBuilder()
//     sb.append('{')
//     var first = true
//     i foreach { case (k, v) =>
//       if (v.isDefined) {
//         if (!first) sb.append(',')
//         else first = false
//         sb.append('"')
//         quote(k, sb)
//         sb.append('"')
//         sb.append(':')
//         sb.append(v)
//       }
//     }
//     sb.append('}')
//     sb.toString
//   }
//   read[Map[String, Option[Int]]](json) == i
// }

  private[this] def quote(s: String, writer: StringBuilder) {
    var i = 0
    val l = s.length
    while (i < l) {
      val c = s(i)
      if (c == '"') writer.append("\\\"")
      else if (c == '\\') writer.append("\\\\")
      else if (c == '\b') writer.append("\\b")
      else if (c == '\f') writer.append("\\f")
      else if (c == '\n') writer.append("\\n")
      else if (c == '\r') writer.append("\\r")
      else if (c == '\t') writer.append("\\t")
      else if ((c >= '\u0000' && c <= '\u001f') || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100')) {
        writer.append("\\u")
        writer.append(HexAlphabet.charAt(c >> 12 & 0x000F))
        writer.append(HexAlphabet.charAt(c >>  8 & 0x000F))
        writer.append(HexAlphabet.charAt(c >>  6 & 0x000F))
        writer.append(HexAlphabet.charAt(c >>  0 & 0x000F))
      } else writer.append(c.toString)
      i += 1
    }
  }
}