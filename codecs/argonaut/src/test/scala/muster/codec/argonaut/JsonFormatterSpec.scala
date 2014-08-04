package muster
package codec
package argonaut

import org.scalacheck.{Gen, Prop}
import org.specs2.specification.Fragments
import org.specs2.{ScalaCheck, Specification}
import _root_.argonaut._, Argonaut._

class JsonFormatterSpec extends Specification with ScalaCheck {
  def is: Fragments =
s2"""
A Json Formatter should
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

  def read[T:Consumer](value: Json) = ArgonautCodec.as[T](value)

  implicit def MapIntDecodeJson[V](implicit e: DecodeJson[V]): DecodeJson[Map[Int, V]] =
    DecodeJson(a =>
      a.fields match {
        case None => DecodeResult.fail("[V]Map[Int, V]", a.history)
        case Some(s) => {
          def spin(x: List[JsonField], m: DecodeResult[Map[Int, V]]): DecodeResult[Map[Int, V]] =
            x match {
              case Nil => m
              case h::t =>
                spin(t, for {
                    mm <- m
                    v <- a.get(h)(e)
                  } yield mm + ((h.toInt, v)))
            }

          spin(s, DecodeResult.ok(Map.empty[Int, V]))
        }
      }
    )

  implicit def MapLongDecodeJson[V](implicit e: DecodeJson[V]): DecodeJson[Map[Long, V]] =
    DecodeJson(a =>
      a.fields match {
        case None => DecodeResult.fail("[V]Map[Long, V]", a.history)
        case Some(s) => {
          def spin(x: List[JsonField], m: DecodeResult[Map[Long, V]]): DecodeResult[Map[Long, V]] =
            x match {
              case Nil => m
              case h::t =>
                spin(t, for {
                    mm <- m
                    v <- a.get(h)(e)
                  } yield mm + ((h.toLong, v)))
            }

          spin(s, DecodeResult.ok(Map.empty[Long, V]))
        }
      }
    )


  val jstringGen = for {
    s <- Gen.alphaStr
  } yield jString(s)

  val jboolGen = for {
    s <- Gen.oneOf(true, false)
  } yield jBool(s)

  val jintGen = for {
    s <- Gen.chooseNum(Int.MinValue, Int.MaxValue)
  } yield jNumber(s)

  val jdoubleGen = for {
    s <- Gen.chooseNum(Double.MinValue, Double.MaxValue)
  } yield jNumber(s)

  val jdecimalGen = for {
    s <- Gen.chooseNum(Double.MinValue, Double.MaxValue)
  } yield jNumber(s)

  val stringListGen = for {
    s <- Gen.listOf(Gen.alphaStr).map(_.map(jString))
  } yield Json.array(s:_*)

  val stringMapGen = {
    val k = Gen.alphaStr.suchThat(ss => ss != null && ss.trim.nonEmpty)
    val v = k.flatMap(kk => Gen.alphaStr.map(vv => (kk, jString(vv))))
    for {
      s <- Gen.listOf(v)
    } yield Json.obj(s:_*)
  }

  val stringProp = Prop.forAll(jstringGen) { jst =>
    read[String](jst) must_== jst.stringOrEmpty
  }
  val boolProp = Prop.forAll(jboolGen) { jst =>
    read[Boolean](jst) must_== jst.bool.get
  }
  val intProp = Prop.forAll(jintGen) { jst =>
    read[Int](jst) must_== jst.numberOrZero
  }
  val doubleProp = Prop.forAll(jdoubleGen) { jst =>
    read[Double](jst) must_== jst.numberOrZero
  }
  val bigdecimalProp = Prop.forAll(jdecimalGen) { jst =>
    read[BigDecimal](jst) must_== jst.numberOrZero
  }

  val stringListProp = Prop.forAll(stringListGen) { jst =>
    read[List[String]](jst) must_== jst.jdecode[List[String]].toOption.get
  }

  val stringMapProp = Prop.forAll(stringMapGen) { jst =>
    read[Map[String, String]](jst) must_== jst.jdecode[Map[String, String]].toOption.get
  }

  import scala.collection.mutable
  val mutableListProp = prop {
    (i: mutable.ListBuffer[Int]) => read[mutable.ListBuffer[Int]](Json.array(i.map(v => jNumber(v)):_*)) must_== i
  }

  val mapGen = {
    for {
      n <- Gen.alphaStr.suchThat(s => s != null && s.trim.nonEmpty)
      m <- Gen.chooseNum(1, 999999999)
      t = (n, jNumber(m))
      r <- Gen.listOf(t)
    } yield Json.obj(r:_*)
  }

  val mapProp = Prop.forAll(mapGen) { (json: Json) =>
    read[Map[String, Int]](json) must_== json.jdecode[Map[String, Int]].toOption.get
  }


    val mapListGen = {
      for {
        n <- Gen.alphaStr.suchThat(s => s != null && s.trim.nonEmpty)
        m <- Gen.listOf(Gen.chooseNum(1, 999999999).map(jNumber(_)))
        t = (n, Json.array(m:_*))
        r <- Gen.listOf(t)
      } yield Json.obj(r:_*)
    }

    val mapListProp = Prop.forAll(mapListGen) { (json: Json) =>

        read[Map[String, List[Int]]](json) must_== json.jdecode[Map[String, List[Int]]].toOption.get
    }

    val intMapGen = {
      for {
        n <- Gen.posNum[Int]
        m <- Gen.chooseNum(1, 999999999)
        t = (n.toString, jNumber(m))
        r <- Gen.listOf(t)
      } yield Json.obj(r:_*)
    }

    val mapIntProp = Prop.forAll(intMapGen) { (json: Json) =>
        read[Map[Int, Int]](json) must_== json.jdecode[Map[Int, Int]].toOption.get
    }

    val intMapListGen = {
      for {
        n <- Gen.posNum[Int]
        m <- Gen.listOf(Gen.chooseNum(1, 999999999).map(jNumber(_)))
        t = (n.toString, Json.array(m:_*))
        r <- Gen.listOf(t)
      } yield Json.obj(r:_*)
    }

    val mapIntListProp = Prop.forAll(intMapListGen) { (json: Json) =>
      read[Map[Int, List[Int]]](json) must_== json.jdecode[Map[Int, List[Int]]].toOption.get
    }

    val longMapGen = {
      for {
        n <- Gen.posNum[Long]
        m <- Gen.chooseNum(1, 999999999)
        t = (n.toString, jNumber(m))
        r <- Gen.listOf(t)
      } yield Json.obj(r:_*)
    }

    val mapLongProp = Prop.forAll(longMapGen) { (json: Json) =>
      read[Map[Long, Int]](json) must_== json.jdecode[Map[Long, Int]].toOption.get
    }

    val longMapListGen = {
      for {
        n <- Gen.posNum[Long]
        m <- Gen.listOf(Gen.chooseNum(1, 999999999).map(jNumber(_)))
        t = (n.toString, Json.array(m:_*))
        r <- Gen.listOf(t)
      } yield Json.obj(r:_*)
    }

    val mapLongListProp = Prop.forAll(longMapListGen) { (json: Json) =>
      read[Map[Long, List[Int]]](json) must_== json.jdecode[Map[Long, List[Int]]].toOption.get
    }
}