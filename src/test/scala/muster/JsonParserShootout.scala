package muster

//import org.scalameter.api._

import scala.io.Source
import com.fasterxml.jackson.databind.ObjectMapper
import org.scalameter.api._
import org.scalameter.CurveData
import org.scalameter.utils.Tree
import org.scalameter.reporting

//import com.fasterxml.jackson.databind.ObjectMapper
//import org.scalameter.{reporting, CurveData, log}
//import org.scalameter.utils.Tree

object Benchmarks {
  val smallJson = Source.fromInputStream(getClass.getResourceAsStream("/small.json")).mkString
  //  val smallJsonGen = Gen.single("small.json")(smallJson)

  val json = Source.fromInputStream(getClass.getResourceAsStream("/larger.json")).mkString // 1.2Mb
  val jsonGen = Gen.single("larger.json")(json)

}

class JsonParsersBenchmark extends com.google.caliper.SimpleBenchmark {

  import Benchmarks._

  val mapper = new ObjectMapper()
  val jsonSmart = new net.minidev.json.parser.JSONParser()


  def timeMusterJacksonParserForLarge(reps: Int): Unit =
    for (i <- 0 to reps)
      Muster.consume.Json.createCursor(json).nextNode()

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
      Muster.consume.Json.createCursor(smallJson).nextNode()

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

trait CursorBench extends PerformanceTest.Quickbenchmark {

  override def reporter = new reporting.LoggingReporter {
    override def report(result: CurveData, persistor: Persistor) {
      org.scalameter.log(s"::Benchmark ${result.context.scope}::")
      for (measurement <- result.measurements) {
        org.scalameter.log(s"${measurement.value}")
      }
      org.scalameter.log("")
    }
    override def report(result: Tree[CurveData], persistor: Persistor) = true
  }

}

class JsonInputCursorBenchmark extends CursorBench {
  import Benchmarks._

  performance of "Json format" in {
    measure method "nextNode"  config (
        exec.benchRuns -> 500
      ) in {
      using(jsonGen) in {
        r => Muster.consume.Json.createCursor(r).nextNode()
      }
    }
  }
}

class Json4sParserBenchmark extends CursorBench {
  import Benchmarks._
  performance of "Json4s Json format" in {
    measure method "nextNode"  config (
        exec.benchRuns -> 500
      ) in {
      using(jsonGen) in {
        r => org.json4s.native.parseJson(org.json4s.StringInput(r))
      }
    }
  }

}


class JacksonParserBenchmark extends CursorBench {
  import Benchmarks._

  performance of "Jackson Json format" in {
    measure method "nextNode"  config (
        exec.benchRuns -> 500
      ) in {
      using(jsonGen) in {
        r => new ObjectMapper().readTree(r)
      }
    }
  }

}

class JsonSmartParserBenchmark extends CursorBench {
  import Benchmarks._
  performance of "Json Smart format" in {
    measure method "nextNode"  config (
        exec.benchRuns -> 500
      ) in {
      using(jsonGen) in {
        r => new net.minidev.json.parser.JSONParser().parse(r)
      }
    }
  }

}

