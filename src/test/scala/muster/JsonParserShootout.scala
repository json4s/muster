package muster

//import org.scalameter.api._

import scala.io.Source
import com.fasterxml.jackson.databind.ObjectMapper

//import com.fasterxml.jackson.databind.ObjectMapper
//import org.scalameter.{reporting, CurveData, log}
//import org.scalameter.utils.Tree

object Benchmarks {
  val smallJson = Source.fromInputStream(getClass.getResourceAsStream("/small.json")).mkString
  //  val smallJsonGen = Gen.single("small.json")(smallJson)

  val json = Source.fromInputStream(getClass.getResourceAsStream("/larger.json")).mkString // 1.2Mb
  //  val jsonGen = Gen.single("larger.json")(json)

  //  case class LoggingReporter() extends Reporter {
  //
  //      def report(result: CurveData, persistor: Persistor) {
  //        // output context
  //        log(s"::Benchmark ${result.context.scope}::")
  ////        for ((key, value) <- result.context.properties.filterKeys(Context.machine.properties.keySet.contains).toSeq.sortBy(_._1)) {
  ////          log(s"$key: $value")
  ////        }
  //
  //        // output measurements
  //        for (measurement <- result.measurements) {
  //          log(s"${measurement.value}")
  //        }
  //
  //        // add a new line
  //        log("")
  //      }
  //
  //      def report(result: Tree[CurveData], persistor: Persistor) = true
  //
  //    }
}

class JsonParsersBenchmark extends com.google.caliper.SimpleBenchmark {

  import Benchmarks._

  val mapper = new ObjectMapper()
  val jsonSmart = new net.minidev.json.parser.JSONParser()


  def timeMusterJacksonParserForLarge(reps: Int): Unit =
    for (i <- 0 to reps)
      Muster.from.Json.createCursor(json).nextNode()

  def timeMusterJsonParserForLarge(reps: Int): Unit =
    for (i <- 0 to reps)
      MusterJson.createCursor(json).nextNode()

  def timeJson4SNativeForLarge(reps: Int): Unit =
    for (i <- 0 to reps)
      org.json4s.native.JsonMethods.parse(json)

  def timeJson4SJacksonForLarge(reps: Int): Unit =
    for (i <- 0 to reps)
      org.json4s.jackson.JsonMethods.parse(json)

  def timeJacksonForLarge(reps: Int): Unit =
    for (i <- 0 to reps)
      mapper.readTree(json)

  def timeJsonSmartForLarge(reps: Int): Unit =
    for (i <- 0 to reps)
      jsonSmart.parse(json)

}

class MediumJsonParsersBenchmark extends com.google.caliper.SimpleBenchmark {

  import Benchmarks._

  val mapper = new ObjectMapper()
  val jsonSmart = new net.minidev.json.parser.JSONParser()


  def timeMusterJacksonParserForLarge(reps: Int): Unit =
    for (i <- 0 to reps)
      Muster.from.Json.createCursor(smallJson).nextNode()

  def timeMusterJsonParserForLarge(reps: Int): Unit =
    for (i <- 0 to reps)
      MusterJson.createCursor(smallJson).nextNode()

  def timeJson4SNativeForLarge(reps: Int): Unit =
    for (i <- 0 to reps)
      org.json4s.native.JsonMethods.parse(smallJson)

  def timeJson4SJacksonForLarge(reps: Int): Unit =
    for (i <- 0 to reps)
      org.json4s.jackson.JsonMethods.parse(smallJson)

  def timeJacksonForLarge(reps: Int): Unit =
    for (i <- 0 to reps)
      mapper.readTree(smallJson)

  def timeJsonSmartForLarge(reps: Int): Unit =
    for (i <- 0 to reps)
      jsonSmart.parse(smallJson)

}

//class JsonInputCursorBenchmark extends PerformanceTest.Quickbenchmark {
//  import Benchmarks._
//
//  performance of "Json format" in {
//    measure method "nextNode"  config (
//        exec.benchRuns -> 500
//      ) in {
//      using(jsonGen) in {
//        r => Json.createCursor(r).nextNode()
//      }
//    }
//  }
////
////  performance of "Json format" in {
////    measure method "nextNode"  config (
////        exec.benchRuns -> 500
////      ) in {
////      using(jsonGen) in {
////        r => org.json4s.native.parseJson(org.json4s.StringInput(json))
////      }
////    }
////  }
////
////  performance of "Json format" in {
////     measure method "nextNode"  config (
////         exec.benchRuns -> 500
////       ) in {
////       using(jsonGen) in {
////         r => new ObjectMapper().readTree(json)
////       }
////     }
////   }
//
////  def persistor: Persistor = Persistor.None
//
//  override def reporter = new reporting.LoggingReporter {
//    override def report(result: CurveData, persistor: Persistor) {
//      log(s"::Benchmark ${result.context.scope}::")
//      for (measurement <- result.measurements) {
//        log(s"${measurement.value}")
//      }
//      log("")
//    }
//    override def report(result: Tree[CurveData], persistor: Persistor) = true
//  }
//}
//
//class Json4sParserBenchmark extends PerformanceTest.Quickbenchmark {
//  import Benchmarks._
//  override def reporter = new reporting.LoggingReporter {
//    override def report(result: CurveData, persistor: Persistor) {
//      log(s"::Benchmark ${result.context.scope}::")
//      for (measurement <- result.measurements) {
//        log(s"${measurement.value}")
//      }
//      log("")
//    }
//    override def report(result: Tree[CurveData], persistor: Persistor) = true
//  }
//  performance of "Json format" in {
//    measure method "nextNode"  config (
//        exec.benchRuns -> 500
//      ) in {
//      using(jsonGen) in {
//        r => org.json4s.native.parseJson(org.json4s.StringInput(r))
//      }
//    }
//  }
//
//}
//
//
//class JacksonParserBenchmark extends PerformanceTest.Quickbenchmark {
//  import Benchmarks._
//  override def reporter = new reporting.LoggingReporter {
//    override def report(result: CurveData, persistor: Persistor) {
//      log(s"::Benchmark ${result.context.scope}::")
//      for (measurement <- result.measurements) {
//        log(s"${measurement.value}")
//      }
//      log("")
//    }
//    override def report(result: Tree[CurveData], persistor: Persistor) = true
//  }
//  performance of "Json format" in {
//    measure method "nextNode"  config (
//        exec.benchRuns -> 500
//      ) in {
//      using(jsonGen) in {
//        r => new ObjectMapper().readTree(r)
//      }
//    }
//  }
//
//}
//
//class JsonSmartParserBenchmark extends PerformanceTest.Quickbenchmark {
//  import Benchmarks._
//  override def reporter = new reporting.LoggingReporter {
//    override def report(result: CurveData, persistor: Persistor) {
//      log(s"::Benchmark ${result.context.scope}::")
//      for (measurement <- result.measurements) {
//        log(s"${measurement.value}")
//      }
//      log("")
//    }
//    override def report(result: Tree[CurveData], persistor: Persistor) = true
//  }
//  performance of "Json format" in {
//    measure method "nextNode"  config (
//        exec.benchRuns -> 500
//      ) in {
//      using(jsonGen) in {
//        r => new net.minidev.json.parser.JSONParser().parse(r)
//      }
//    }
//  }
//
//}

