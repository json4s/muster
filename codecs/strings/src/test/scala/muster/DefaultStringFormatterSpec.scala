package muster

import muster.codec.string.StringRenderer

object TestStringFormat extends StringRenderer

class DefaultStringFormatterSpec extends {val format = TestStringFormat } with  StringOutputFormatterSpec {

  val listProp = writerProp { (fmt, lst: List[Int]) =>
    fmt.startArray("List")
    lst foreach fmt.int
    fmt.endArray()
  }

  val objectProp = prop { (obj: Category) =>
    withFormatter { fmt =>
      fmt.startObject("Category")
      fmt.startField("id")
      fmt.int(obj.id)
      fmt.startField("name")
      fmt.string(obj.name)
      fmt.endObject()
      fmt.result must_== s"""Category(id: ${obj.id}, name: "${obj.name}")"""
    }
  }

  val is = br ^ "The default string formatter should" ^ br ^ sharedFragments ^ end

}

