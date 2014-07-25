package muster
package codec
package json4s

import org.json4s._
import org.scalacheck.{Gen, Prop}
import org.specs2.specification.Fragments
import org.specs2.{ScalaCheck, Specification}

class JValueFormatterSpec extends Specification with ScalaCheck {
  def is: Fragments =
s2"""
A JValue Formatter should
  read a string property $stringProp
  read a bool property $boolProp
  read a int property $intProp
  read a double property $doubleProp
  read a decimal property $bigdecimalProp
  read a list property $stringListProp
  read a mutable list property $mutableListProp
  read a string map property $stringMapProp
  read a map property $mapProp
  read a map list property $mapListProp
  read an int map $mapIntProp
  read an int list map $mapIntListProp
  read a long map $mapLongProp
  read a long list map $mapLongListProp
"""

  def read[T:Consumer](value: JValue) = JValueFormat.as[T](value, SingleValue)

  val jstringGen = for {
    s <- Gen.alphaStr
  } yield JString(s)

  val jboolGen = for {
    s <- Gen.oneOf(true, false)
  } yield JBool(s)

  val jintGen = for {
    s <- Gen.chooseNum(Int.MinValue, Int.MaxValue)
  } yield JInt(s)

  val jdoubleGen = for {
    s <- Gen.chooseNum(Double.MinValue, Double.MaxValue)
  } yield JDouble(s)

  val jdecimalGen = for {
    s <- Gen.chooseNum(Double.MinValue, Double.MaxValue)
  } yield JDecimal(BigDecimal(s))

  val stringListGen = for {
    s <- Gen.listOf(Gen.alphaStr).map(_.map(JString))
  } yield JArray(s)

  val stringMapGen = {
    val k = Gen.alphaStr.suchThat(ss => ss != null && ss.trim.nonEmpty)
    val v = k.flatMap(kk => Gen.alphaStr.map(vv => (kk, JString(vv))))
    for {
      s <- Gen.listOf(v)
    } yield JObject(s)
  }

  val stringProp = Prop.forAll(jstringGen) { jst =>
    read[String](jst) must_== jst.s
  }
  val boolProp = Prop.forAll(jboolGen) { jst =>
    read[Boolean](jst) must_== jst.value
  }
  val intProp = Prop.forAll(jintGen) { jst =>
    read[Int](jst) must_== jst.num
  }
  val doubleProp = Prop.forAll(jdoubleGen) { jst =>
    read[Double](jst) must_== jst.num
  }
  val bigdecimalProp = Prop.forAll(jdecimalGen) { jst =>
    read[BigDecimal](jst) must_== jst.num
  }

  val stringListProp = Prop.forAll(stringListGen) { jst =>
    read[List[String]](jst) must_== jst \\ classOf[JString]
  }

  val stringMapProp = Prop.forAll(stringMapGen) { jst =>
    read[Map[String, String]](jst) must_== jst.values.map(kv => kv._1 -> kv._2.asInstanceOf[String])
  }

  import scala.collection.mutable
  val mutableListProp = prop {
    (i: mutable.ListBuffer[Int]) => read[mutable.ListBuffer[Int]](JArray(i.map(JInt(_)).toList)) must_== i
  }

  val mapGen = {
    for {
      n <- Gen.alphaStr.suchThat(s => s != null && s.trim.nonEmpty)
      m <- Gen.chooseNum(1, 999999999)
      t = (n, JInt(m))
      r <- Gen.listOf(t)
    } yield JObject(r)
  }

  val mapProp = Prop.forAll(mapGen) { (json: JObject) =>
    read[Map[String, Int]](json) must_== json.values.map(kv => kv._1 -> kv._2.asInstanceOf[BigInt].toInt)
  }


    val mapListGen = {
      for {
        n <- Gen.alphaStr.suchThat(s => s != null && s.trim.nonEmpty)
        m <- Gen.listOf(Gen.chooseNum(1, 999999999).map(JInt(_)))
        t = (n, JArray(m))
        r <- Gen.listOf(t)
      } yield JObject(r)
    }

    val mapListProp = Prop.forAll(mapListGen) { (json: JObject) =>

        read[Map[String, List[Int]]](json) must_== json.values.map(kv => kv._1 -> kv._2.asInstanceOf[List[BigInt]].map(_.toInt))
    }

    val intMapGen = {
      for {
        n <- Gen.posNum[Int]
        m <- Gen.chooseNum(1, 999999999)
        t = (n.toString, JInt(m))
        r <- Gen.listOf(t)
      } yield JObject(r)
    }

    val mapIntProp = Prop.forAll(intMapGen) { (json: JObject) =>
        read[Map[Int, Int]](json) must_== json.values.map(kv => kv._1.toInt -> kv._2.asInstanceOf[BigInt].toInt)
    }

    val intMapListGen = {
      for {
        n <- Gen.posNum[Int]
        m <- Gen.listOf(Gen.chooseNum(1, 999999999).map(JInt(_)))
        t = (n.toString, JArray(m))
        r <- Gen.listOf(t)
      } yield JObject(r)
    }

    val mapIntListProp = Prop.forAll(intMapListGen) { (json: JObject) =>
      read[Map[Int, List[Int]]](json) must_== json.values.map(kv => kv._1.toInt -> kv._2.asInstanceOf[List[BigInt]].map(_.toInt))
    }

    val longMapGen = {
      for {
        n <- Gen.posNum[Long]
        m <- Gen.chooseNum(1, 999999999)
        t = (n.toString, JInt(m))
        r <- Gen.listOf(t)
      } yield JObject(r)
    }

    val mapLongProp = Prop.forAll(longMapGen) { (json: JObject) =>
      read[Map[Long, Int]](json) must_== json.values.map(kv => kv._1.toLong -> kv._2.asInstanceOf[BigInt].toInt)
    }

    val longMapListGen = {
      for {
        n <- Gen.posNum[Long]
        m <- Gen.listOf(Gen.chooseNum(1, 999999999).map(JInt(_)))
        t = (n.toString, JArray(m))
        r <- Gen.listOf(t)
      } yield JObject(r)
    }

    val mapLongListProp = Prop.forAll(longMapListGen) { (json: JObject) =>
      read[Map[Long, List[Int]]](json) must_== json.values.map(kv => kv._1.toLong -> kv._2.asInstanceOf[List[BigInt]].map(_.toInt))
    }
}
