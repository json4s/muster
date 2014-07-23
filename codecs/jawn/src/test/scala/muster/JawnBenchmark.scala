package muster

import org.scalameter.api._
import muster.codec.json.{Benchmarks, CursorBench}

class JawnBenchmark extends CursorBench {
  import Benchmarks._

  performance of "Muster Jackson format" in {
    measure method "nextNode"  config (
        exec.benchRuns -> 500
      ) in {
      using(jsonGen) in {
        r => codec.jawn.JsonFormat.createCursor(r, SingleValue).nextNode()
      }
    }
  }
}