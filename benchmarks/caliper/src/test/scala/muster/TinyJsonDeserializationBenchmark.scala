package muster

import java.util.Date

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import muster.codec.json.Benchmarks._
import muster.jackson.util.ISO8601Utils
import org.json4s.{DateFormat, DefaultFormats}

import scala.util.Try

// For data from: http://www.json-generator.com/

object TestFriend {
  implicit val testFriendConsumer: Consumer[TestFriend] = Consumer.consumer[TestFriend]
  implicit val testFriendProducer: Producer[TestFriend] = Producer.producer[TestFriend]
}
case class TestFriend(id: Int, name: String)
object TestJson {
  implicit val testJsonConsumer: Consumer[TestJson] = Consumer.consumer[TestJson]
  implicit val testJsonProducer: Producer[TestJson] = Producer.producer[TestJson]
}
case class TestJson(
  id: String,
  index: Int,
  guid: String,
  isActive: Boolean,
  balance: String,
  picture: String,
  age: Int,
  eyeColor: String, // TODO: Replace with enumeration
  name: String,
  gender: String,
  company: String,
  email: String,
  phone: String,
  address: String,
  about: String,
  registered: Date,
  latitude: Double,
  longitude: Double,
  tags: Seq[String],
  friends: Seq[TestFriend],
  greeting: String,
  favoriteFruit: String // TODO: replace with enumeration
)

class TinyJsonDeserializationBenchmark extends  com.google.caliper.SimpleBenchmark {
  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  val fmts = new DefaultFormats {
    override val dateFormat: DateFormat = new DateFormat {
      def parse(s: String): Option[Date] = Try(ISO8601Utils.parse(s)).toOption

      def format(d: Date): String = ISO8601Utils.format(d, true)
    }
  }

  def timeJson4sJacksonForTiny(reps: Int): Unit =
    for (i <- 0 to reps) org.json4s.jackson.Serialization.read[List[TestJson]](tinyJson)(fmts, manifest[List[TestJson]])

  def timeJacksonForTiny(reps: Int): Unit =
    for (i <- 0 to reps) mapper.readValue(tinyJson, classOf[List[TestJson]])

  def timeMusterJawnParserForTiny(reps: Int): Unit =
    for (i <- 0 to reps) codec.jawn.JawnCodec.as[List[TestJson]](tinyJson)

  def timeMusterJacksonParserForTiny(reps: Int): Unit =
    for (i <- 0 to reps) codec.jackson.JacksonCodec.as[List[TestJson]](tinyJson)

  def timeJson4sJacksonArrayForTiny(reps: Int): Unit =
    for (i <- 0 to reps) org.json4s.jackson.Serialization.read[Array[TestJson]](tinyJson)(fmts, manifest[Array[TestJson]])

  def timeJacksonArrayForTiny(reps: Int): Unit =
    for (i <- 0 to reps) mapper.readValue(tinyJson, classOf[Array[TestJson]])

  def timeMusterJawnParserArrayForTiny(reps: Int): Unit =
    for (i <- 0 to reps) codec.jawn.JawnCodec.as[Array[TestJson]](tinyJson)

  def timeMusterJacksonParserArrayForTiny(reps: Int): Unit =
    for (i <- 0 to reps) codec.jackson.JacksonCodec.as[Array[TestJson]](tinyJson)

}