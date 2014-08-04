package muster
package codec
package play

import org.scalacheck.{Gen, Prop}
import org.specs2.specification.Fragments
import org.specs2.{ScalaCheck, Specification}
import _root_.play.api.libs.json._

class JsValueFormatterSpec extends Specification with ScalaCheck {
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

  def read[T:Consumer](value: JsValue) = PlayJsonCodec.as[T](value)

  val jstringGen = for {
    s <- Gen.alphaStr
  } yield JsString(s)

  val jboolGen = for {
    s <- Gen.oneOf(true, false)
  } yield JsBoolean(s)

  val JsNumberGen = for {
    s <- Gen.chooseNum(Int.MinValue, Int.MaxValue)
  } yield JsNumber(s)

  val jdoubleGen = for {
    s <- Gen.chooseNum(Double.MinValue, Double.MaxValue)
  } yield JsNumber(s)

  val jdecimalGen = for {
    s <- Gen.chooseNum(Double.MinValue, Double.MaxValue)
  } yield JsNumber(BigDecimal(s))

  val stringListGen = for {
    s <- Gen.listOf(Gen.alphaStr).map(_.map(JsString))
  } yield JsArray(s)

  val stringMapGen = {
    val k = Gen.alphaStr.suchThat(ss => ss != null && ss.trim.nonEmpty)
    val v = k.flatMap(kk => Gen.alphaStr.map(vv => (kk, JsString(vv))))
    for {
      s <- Gen.listOf(v)
    } yield JsObject(s)
  }

  val stringProp = Prop.forAll(jstringGen) { jst =>
    read[String](jst) must_== jst.value
  }
  val boolProp = Prop.forAll(jboolGen) { jst =>
    read[Boolean](jst) must_== jst.value
  }
  val intProp = Prop.forAll(JsNumberGen) { jst =>
    read[Int](jst) must_== jst.value.toInt
  }
  val doubleProp = Prop.forAll(jdoubleGen) { jst =>
    read[Double](jst) must_== jst.value.toDouble
  }
  val bigdecimalProp = Prop.forAll(jdecimalGen) { jst =>
    read[BigDecimal](jst) must_== jst.value
  }

  val stringListProp = Prop.forAll(stringListGen) { jst =>
    read[List[String]](jst) must_== jst.as[List[String]]
  }

  val stringMapProp = Prop.forAll(stringMapGen) { jst =>
    read[Map[String, String]](jst) must_== jst.value.map(kv => kv._1 -> kv._2.as[String])
  }

  import scala.collection.mutable
  val mutableListProp = prop {
    (i: mutable.ListBuffer[Int]) => read[mutable.ListBuffer[Int]](JsArray(i.map(JsNumber(_)).toList)) must_== i
  }

  val mapGen = {
    for {
      n <- Gen.alphaStr.suchThat(s => s != null && s.trim.nonEmpty)
      m <- Gen.chooseNum(1, 999999999)
      t = (n, JsNumber(m))
      r <- Gen.listOf(t)
    } yield JsObject(r)
  }

  val mapProp = Prop.forAll(mapGen) { (json: JsObject) =>
    read[Map[String, Int]](json) must_== json.value.map(kv => kv._1 -> kv._2.as[Int])
  }


    val mapListGen = {
      for {
        n <- Gen.alphaStr.suchThat(s => s != null && s.trim.nonEmpty)
        m <- Gen.listOf(Gen.chooseNum(1, 999999999).map(JsNumber(_)))
        t = (n, JsArray(m))
        r <- Gen.listOf(t)
      } yield JsObject(r)
    }

    val mapListProp = Prop.forAll(mapListGen) { (json: JsObject) =>

        read[Map[String, List[Int]]](json) must_== json.value.map(kv => kv._1 -> kv._2.as[List[Int]])
    }

    val intMapGen = {
      for {
        n <- Gen.posNum[Int]
        m <- Gen.chooseNum(1, 999999999)
        t = (n.toString, JsNumber(m))
        r <- Gen.listOf(t)
      } yield JsObject(r)
    }

    val mapIntProp = Prop.forAll(intMapGen) { (json: JsObject) =>
        read[Map[Int, Int]](json) must_== json.value.map(kv => kv._1.toInt -> kv._2.as[Int])
    }

    val intMapListGen = {
      for {
        n <- Gen.posNum[Int]
        m <- Gen.listOf(Gen.chooseNum(1, 999999999).map(JsNumber(_)))
        t = (n.toString, JsArray(m))
        r <- Gen.listOf(t)
      } yield JsObject(r)
    }

    val mapIntListProp = Prop.forAll(intMapListGen) { (json: JsObject) =>
      read[Map[Int, List[Int]]](json) must_== json.value.map(kv => kv._1.toInt -> kv._2.as[List[Int]])
    }

    val longMapGen = {
      for {
        n <- Gen.posNum[Long]
        m <- Gen.chooseNum(1, 999999999)
        t = (n.toString, JsNumber(m))
        r <- Gen.listOf(t)
      } yield JsObject(r)
    }

    val mapLongProp = Prop.forAll(longMapGen) { (json: JsObject) =>
      read[Map[Long, Int]](json) must_== json.value.map(kv => kv._1.toLong -> kv._2.as[Int])
    }

    val longMapListGen = {
      for {
        n <- Gen.posNum[Long]
        m <- Gen.listOf(Gen.chooseNum(1, 999999999).map(JsNumber(_)))
        t = (n.toString, JsArray(m))
        r <- Gen.listOf(t)
      } yield JsObject(r)
    }

    val mapLongListProp = Prop.forAll(longMapListGen) { (json: JsObject) =>
      read[Map[Long, List[Int]]](json) must_== json.value.map(kv => kv._1.toLong -> kv._2.as[List[Int]])
    }
}