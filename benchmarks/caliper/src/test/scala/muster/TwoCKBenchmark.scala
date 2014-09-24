package muster

import java.util.Date

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.caliper.SimpleBenchmark
import muster.codec.json.Benchmarks._
import muster.jackson.util.ISO8601Utils
import org.json4s.{DateFormat, DefaultFormats}

import scala.util.Try

class TwoCKBenchmark extends SimpleBenchmark {
  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  val fmts = new DefaultFormats {
    override val dateFormat: DateFormat = new DateFormat {
      def parse(s: String): Option[Date] = Try(ISO8601Utils.parse(s)).toOption

      def format(d: Date): String = ISO8601Utils.format(d, true)
    }
  }

  def timeJson4sJacksonFor200k(reps: Int): Unit =
    for (i <- 0 to reps) org.json4s.jackson.Serialization.read[List[TestJson]](twoCKJson)(fmts, manifest[List[TestJson]])

  def timeJacksonFor200k(reps: Int): Unit =
    for (i <- 0 to reps) mapper.readValue(twoCKJson, classOf[List[TestJson]])

  def timeMusterJawnParserFor200k(reps: Int): Unit =
    for (i <- 0 to reps) codec.jawn.JawnCodec.as[List[TestJson]](twoCKJson)

  def timeMusterJacksonParserFor200k(reps: Int): Unit =
    for (i <- 0 to reps) codec.jackson.JacksonCodec.as[List[TestJson]](twoCKJson)

  def timeJson4sJacksonArrayFor200k(reps: Int): Unit =
    for (i <- 0 to reps) org.json4s.jackson.Serialization.read[Array[TestJson]](twoCKJson)(fmts, manifest[Array[TestJson]])

  def timeJacksonArrayFor200k(reps: Int): Unit =
    for (i <- 0 to reps) mapper.readValue(twoCKJson, classOf[Array[TestJson]])

  def timeMusterJawnParserArrayFor200k(reps: Int): Unit =
    for (i <- 0 to reps) codec.jawn.JawnCodec.as[Array[TestJson]](twoCKJson)

  def timeMusterJacksonParserArrayFor200k(reps: Int): Unit =
    for (i <- 0 to reps) codec.jackson.JacksonCodec.as[Array[TestJson]](twoCKJson)
  
}