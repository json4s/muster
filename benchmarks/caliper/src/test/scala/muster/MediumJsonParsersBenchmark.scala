package muster

import com.fasterxml.jackson.databind.ObjectMapper
import muster.codec.json.{api, MusterJson}

class MediumJsonParsersBenchmark extends com.google.caliper.SimpleBenchmark {

   import Benchmarks._

   val mapper = new ObjectMapper()
   val jsonSmart = new net.minidev.json.parser.JSONParser()


   def timeMusterJacksonParserForLarge(reps: Int): Unit =
     for (i <- 0 to reps) api.JsonFormat.createCursor(smallJson).nextNode()

   def timeMusterJsonParserForLarge(reps: Int): Unit =
     for (i <- 0 to reps) MusterJson.createCursor(smallJson).nextNode()

   def timeJson4sNativeForLarge(reps: Int): Unit =
     for (i <- 0 to reps) org.json4s.native.JsonMethods.parse(smallJson)

   def timeJson4sJacksonForLarge(reps: Int): Unit =
     for (i <- 0 to reps) org.json4s.jackson.JsonMethods.parse(smallJson)

   def timeJacksonForLarge(reps: Int): Unit =
     for (i <- 0 to reps) mapper.readTree(smallJson)

   def timeJsonSmartForLarge(reps: Int): Unit =
     for (i <- 0 to reps) jsonSmart.parse(smallJson)

 }