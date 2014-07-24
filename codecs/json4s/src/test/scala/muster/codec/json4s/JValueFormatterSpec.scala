package muster
package codec
package json4s

import muster.{Mode, InputFormat, Consumable, InputCursor}
import muster.codec.json.{JsonConsumerSpec, JsonSerializationSpec}
import org.json4s.JsonAST._
import org.scalacheck.{Gen, Prop}
import org.specs2.{ScalaCheck, Specification}
import org.specs2.specification.Fragments

class JValueFormatterSpec extends Specification with ScalaCheck {
  def is: Fragments =
s2"""
A JValue Formatter should
  read a string property $stringProp
  read a bool property $boolProp
  read a int property $intProp
  read a double property $doubleProp
  read a decimal property $bigdecimalProp
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

  
}
