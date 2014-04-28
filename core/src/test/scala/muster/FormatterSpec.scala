package muster

import org.specs2.{ScalaCheck, Specification}
import java.util.TimeZone
import org.scalacheck.{Gen, Arbitrary}

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