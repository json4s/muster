package muster
package codec
package json

import org.scalameter.api._
import org.scalameter.utils.Tree
import org.scalameter.{CurveData, reporting}

import scala.io.Source

object Benchmarks {
  val smallJson = Source.fromInputStream(getClass.getResourceAsStream("/small.json")).mkString
  //  val smallJsonGen = Gen.single("small.json")(smallJson)

  val json = Source.fromInputStream(getClass.getResourceAsStream("/larger.json")).mkString // 1.2Mb
  val jsonGen = Gen.single("larger.json")(json)

  val tinyJson = Source.fromInputStream(getClass.getResourceAsStream("/tiny.json")).mkString
  val twoCKJson = Source.fromInputStream(getClass.getResourceAsStream("/200k.json")).mkString

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



class JsonSmartParserBenchmark extends CursorBench {
  import Benchmarks._
  performance of "Json Smart format" in {
    measure method "nextNode"  config (
        exec.benchRuns -> 500
      ) in {
      using(jsonGen) in {
        r => new net.minidev.json.parser.JSONParser(-1)
      }
    }
  }

}