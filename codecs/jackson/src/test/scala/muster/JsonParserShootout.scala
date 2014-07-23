package muster

//import org.scalameter.api._

import scala.io.Source
import com.fasterxml.jackson.databind.ObjectMapper
import org.scalameter.api._
import org.scalameter.CurveData
import org.scalameter.utils.Tree
import org.scalameter.reporting
import codec.json._

//import com.fasterxml.jackson.databind.ObjectMapper
//import org.scalameter.{reporting, CurveData, log}
//import org.scalameter.utils.Tree


class JacksonInputCursorBenchmark extends CursorBench {
  import Benchmarks._

  performance of "Muster Jackson format" in {
    measure method "nextNode"  config (
        exec.benchRuns -> 500
      ) in {
      using(jsonGen) in {
        r => codec.jackson.JsonFormat.createCursor(r, SingleValue).nextNode()
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


