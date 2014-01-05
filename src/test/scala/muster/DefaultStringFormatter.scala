package muster

import org.scalacheck._
import java.util.{TimeZone, Date}

object DefaultStringFormatter extends Properties("DefaultStringFormatter") {

  import Prop.forAll

  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  val format = Muster.produce.String
  //  implicit lazy val arbDateTime: Arbitrary[DateTime] = Arbitrary(for {
  //    l <- Arbitrary.arbitrary[Long]
  //  } yield new DateTime(System.currentTimeMillis() + l, DateTimeZone.UTC))

  implicit lazy val arbCategory: Arbitrary[Category] = Arbitrary(for {
    id <- Arbitrary.arbitrary[Int]
    nm <- Gen.alphaStr
  } yield Category(id, nm))


  property("byte") = forAll { (x: Byte) =>
    val fmt = format.createFormatter
    fmt.byte(x)
    val r = fmt.result
    fmt.close()
    r == x.toString
  }

  property("short") = forAll { (x: Short) =>
    val fmt = format.createFormatter
    fmt.short(x)
    val r = fmt.result
    fmt.close()
    r == x.toString
  }

  property("int") = forAll { (x: Int) =>
    val fmt = format.createFormatter
    fmt.int(x)
    val r = fmt.result
    fmt.close()
    r == x.toString
  }

  property("long") = forAll { (x: Long) =>
    val fmt = format.createFormatter
    fmt.long(x)
    val r = fmt.result
    fmt.close()
    r == x.toString
  }

  property("bigInt") = forAll { (x: BigInt) =>
    val fmt = format.createFormatter
    fmt.bigInt(x)
    val r = fmt.result
    fmt.close()
    r == x.toString
  }

  property("float") = forAll { (x: Float) =>
    val fmt = format.createFormatter
    fmt.float(x)
    val r = fmt.result
    fmt.close()
    r == x.toString
  }

  property("double") = forAll { (x: Double) =>
    val fmt = format.createFormatter
    fmt.double(x)
    val r = fmt.result
    fmt.close()
    r == x.toString
  }

  property("bigDecimal") = forAll { (x: BigDecimal) =>
    val fmt = format.createFormatter
    fmt.bigDecimal(x)
    val r = fmt.result
    fmt.close()
    r == x.toString
  }

  property("boolean") = forAll { (x: Boolean) =>
    val fmt = format.createFormatter
    fmt.boolean(x)
    val r = fmt.result
    fmt.close()
    r == x.toString
  }

  property("date") = forAll { (x: Date) =>
    val fmt = format.createFormatter
    fmt.date(x)
    val r = fmt.result
    fmt.close()
    r == format.dateFormat.format(x)
  }
  //
  //  property("dateTime") = forAll { (x: DateTime) =>
  //    val fmt = format.createFormatter
  //    fmt.dateTime(x)
  //    val r = fmt.result
  //    fmt.close()
  //    r == x.toString(ISODateTimeFormat.dateTimeNoMillis.withZone(DateTimeZone.UTC))
  //  }

  property("string") = forAll(Gen.alphaStr) { (x: String) =>
    val fmt = format.createFormatter
    fmt.string(x)
    val r = fmt.result
    fmt.close()
    r == "\"" + x + "\""
  }

  property("list") = forAll(Gen.nonEmptyListOf(Gen.posNum[Int])) { lst =>
    val fmt = format.createFormatter
    fmt.startArray("List")
    lst foreach fmt.int
    fmt.endArray()
    val r = fmt.result
    fmt.close()
    r == lst.toString()
  }

  property("object") = forAll { (obj: Category) =>
    val fmt = format.createFormatter
    fmt.startObject("Category")
    fmt.startField("id")
    fmt.int(obj.id)
    fmt.startField("name")
    fmt.string(obj.name)
    fmt.endObject()
    val r = fmt.result
    fmt.close()
    r == s"""Category(id: ${obj.id}, name: "${obj.name}")"""
  }


}