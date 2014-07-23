package muster

import com.fasterxml.jackson.databind.ObjectMapper
import muster.codec.json._

class MediumJsonParsersBenchmark extends com.google.caliper.SimpleBenchmark {

   import Benchmarks._

   val mapper = new ObjectMapper()
   val jsonSmart = new net.minidev.json.parser.JSONParser()


   def timeMusterJacksonParserForLarge(reps: Int): Unit =
     for (i <- 0 to reps) codec.jackson.JsonFormat.createCursor(smallJson, SingleValue).nextNode()

   def timeMusterJsonParserForLarge(reps: Int): Unit =
     for (i <- 0 to reps) codec.jackson.JsonFormat.createCursor(smallJson, SingleValue).nextNode()

   def timeJson4sNativeForLarge(reps: Int): Unit =
     for (i <- 0 to reps) org.json4s.native.JsonMethods.parse(smallJson)

   def timeJson4sJacksonForLarge(reps: Int): Unit =
     for (i <- 0 to reps) org.json4s.jackson.JsonMethods.parse(smallJson)

   def timeJacksonForLarge(reps: Int): Unit =
     for (i <- 0 to reps) mapper.readTree(smallJson)

   def timeJsonSmartForLarge(reps: Int): Unit =
     for (i <- 0 to reps) jsonSmart.parse(smallJson)

 }