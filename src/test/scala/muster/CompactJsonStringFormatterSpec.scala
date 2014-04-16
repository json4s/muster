package muster

//import org.scalacheck._
import java.util.{TimeZone, Date}
import org.specs2.{ScalaCheck, Specification}
import org.scalacheck.{Arbitrary, Gen}
//import org.joda.time.DateTime
//import org.joda.time.DateTimeZone

case class Category(id: Int, name: String)

trait FormatterSpec[T] extends Specification with ScalaCheck {
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  val format: OutputFormat[T]
//
//   implicit lazy val arbDateTime: Arbitrary[DateTime] = Arbitrary(for {
//     l <- Arbitrary.arbitrary[Long]
//   } yield new DateTime(System.currentTimeMillis() + l, DateTimeZone.UTC))

  implicit lazy val arbCategory: Arbitrary[Category] = Arbitrary(for {
    id <- Arbitrary.arbInt.arbitrary
    nm <- Gen.alphaStr
  } yield Category(id, nm))

  def withFormatter[R](fn: format.Formatter => R): R = {
    val fmt = format.createFormatter
    try {
      fn(fmt)
    } finally {
      fmt.close()
    }
  }
}

class CompactJsonStringFormatterSpec extends {val format = Muster.produce.Json} with StringOutputFormatterSpec {

  val listProp = prop { (lst: List[Int]) =>
    withFormatter { fmt =>
      fmt.startArray("List")
      lst foreach fmt.int
      fmt.endArray()
      fmt.result must_== lst.mkString("[", ",", "]")
    }
  }

  val objectProp = prop { (obj: Category) =>
    withFormatter { fmt =>
      fmt.startObject("Category")
      fmt.startField("id")
      fmt.int(obj.id)
      fmt.startField("name")
      fmt.string(obj.name)
      fmt.endObject()
      fmt.result must_== s"""{"id":${obj.id},"name":"${obj.name}"}"""
    }
  }

  val is = "The compact json string formatter should" ^ br ^ sharedFragments ^ end
}