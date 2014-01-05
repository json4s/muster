package muster

import org.specs2.mutable.Specification
import org.json4s._
import java.util.{TimeZone, Date}
import muster.Ast.ObjectNode

object Aliased {
  type Foo = Junk
//  object WithAlias {
//    implicit val WithAliasConsumer = Consumer.consumer[WithAlias]
//  }

  case class WithAlias(in: Foo)
}

class Ac {
  type Foo = Junk
  object WithAlias {
    implicit val WithAliasConsumer = Consumer.consumer[WithAlias]
  }
  case class WithAlias(in: Foo)
  case class NoAlias(in: Junk)
}
class Ac2 {
  type Foo = Junk
  case class WithAlias(in: Foo)
  case class NoAlias(in: Junk)
}

class JacksonDeserializationSpec extends Specification {

  implicit val defaultFormats = DefaultFormats
  val format = Muster.from.Json

  val refJunk = Junk(2,"cats")
  val refJunkDict: String = org.json4s.jackson.Serialization.write(refJunk)

  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  type Foo = Junk
  case class WithAlias(in: Foo)


  def read[T:Consumer](value: String) = format.from[T](value)

  "Muster.from.Json" should {
//    "read a dateTime" in {
//      val date = DateTime.now
//      val ds = Muster.from.JsonString.dateFormat.print(new DateTime(date))
//      read[DateTime]("\""+ds+"\"") must_== Muster.from.JsonString.dateFormat.parseDateTime(ds)
//    }
    "read a date" in {
      val date = new Date()
      val ds = SafeSimpleDateFormat.Iso8601Formatter.format(date)
      read[Date]("\""+ds+"\"") must_== SafeSimpleDateFormat.Iso8601Formatter.parse(ds)
    }

    "read a symbol" in {
      val sym = 'a_symbol_of_sorts
      val js = "\""+sym.name+"\""
      read[Symbol](js) must_== sym
    }

    "read a map with option values" in {
      val dict = Map("one" -> 1, "two" -> null, "three" -> 394)
      val json = org.json4s.jackson.Serialization.write(dict)
      read[Map[String, Option[Int]]](json) must_== Map("one" -> Some(1), "two" -> None, "three" -> Some(394))
    }
    "read a map with a list of option values" in {
      val dict = Map("one" -> List(1), "two" -> List(3, null, 4), "three" -> List(394))
      val json = org.json4s.jackson.Serialization.write(dict)
      read[Map[String, List[Option[Int]]]](json) must_== Map("one" -> List(Some(1)), "two" -> List(Some(3),None, Some(4)), "three" -> List(Some(394)))
    }

    "read a very simple case class" in {
      val js = """{"id":1,"name":"Tom"}"""
      read[Friend](js) must_== Friend(1, "Tom")
    }

    "read a very simple case class" in {
      val js = """{"one":1,"two":"Tom"}"""
      read[Simple](js) must_== Simple(1, "Tom")
    }

    "read a very simple case class with an option field with the value provided" in {
      val js = """{"one":1,"two":"Tom"}"""
      read[WithOption](js) must_== WithOption(1, Some("Tom"))
    }

    "read a very simple case class with an option field with null" in {
      val js = """{"one":1,"two":null}"""
      read[WithOption](js) must_== WithOption(1, None)
    }

    "read a very simple case class with an option field omitted" in {
      val js = """{"one":1}"""
      read[WithOption](js) must_== WithOption(1, None)
    }

    "read list of simple case classes" in {
      val js = """[{"one":1,"two":"hello"}, {"one":2,"two":"world"}]"""
      read[List[Simple]](js) must_== List(Simple(1, "hello"), Simple(2, "world"))
    }

    "read a case class with a single list" in {
      val js = """{"lst":[1,2,3]}"""
      read[WithList](js) must_== WithList(List(1,2,3))
    }

    "read an object with list and map" in {
      val js = """{"lst":[1,2,3], "map":{"foo":1,"bar":2}}"""
      read[ObjWithListMap](js) must_== ObjWithListMap(List(1,2,3), Map("foo" -> 1, "bar" -> 2))
    }

    "read an object with a date" in {
      val date = new Date
      val ds = SafeSimpleDateFormat.Iso8601Formatter.format(date)
      val pd = SafeSimpleDateFormat.Iso8601Formatter.parse(ds)
      val js = s"""{"date":"$ds"}"""
      read[WithDate](js) must_== WithDate(pd)
    }
//    "read an object with a datetime" in {
//      val date = DateTime.now
//      val ds = Muster.from.JsonString.dateFormat.print(date)
//      val pd = Muster.from.JsonString.dateFormat.parseDateTime(ds)
//      val js = s"""{"date":"$ds"}"""
//      read[WithDateTime](js) must_== WithDateTime(pd)
//    }

    "read an object with a Symbol" in {
      val js = """{"symbol":"baz"}"""
      read[WithSymbol](js) must_== WithSymbol('baz)
    }

    "read a NotSimple class" in {
      val js = """{"one":456,"simple":{"one":1,"two":"Tom"}}"""
      read[NotSimple](js) must_== NotSimple(456, Simple(1, "Tom"))
    }

    val junkJson = """{"in1":123,"in2":"456"}"""
    val junk = Junk(123, "456")
    val thingWithJunkJson = s"""{"name":"foo","junk":$junkJson}"""
    val thingWithJunk = ThingWithJunk("foo", junk)
    "read a ThingWithJunk" in {
      read[ThingWithJunk](thingWithJunkJson) must_== thingWithJunk
    }

    "read type aliased thing with junk when alias is defined in a package object" in {
      read[aliasing.WithAlias](s"""{"in":{"in1":123,"in2":"456"}}""") must_== aliasing.WithAlias(junk)
    }

    "read type aliased thing with junk when alias is defined in an object" in {
      read[Aliased.WithAlias](s"""{"in":{"in1":123,"in2":"456"}}""") must_== Aliased.WithAlias(junk)
    }

    "read type aliased thing with junk when alias is defined in this class" in {
      read[this.WithAlias](s"""{"in":{"in1":123,"in2":"456"}}""") must_== this.WithAlias(junk)
    }

    "read type aliased thing with junk when alias is defined in another class and companion object is used to invoke the macro" in {
      val ac = new Ac
      read[ac.WithAlias](s"""{"in":{"in1":123,"in2":"456"}}""") must_== ac.WithAlias(junk)
    }

//    "read type aliased thing with junk when alias is defined in another class without companion object" in {
//      val ac = new Ac2
//      read[ac.WithAlias](s"""{"in":{"in1":123,"in2":"456"}}""") must_== ac.WithAlias(junk)
//    }.pendingUntilFixed

    "read a crazy thing" in {
      val js = s"""{"name":"bar","thg":$thingWithJunkJson}"""
      read[Crazy](js) must_== Crazy("bar", thingWithJunk)
    }

    "read an option inside an option for a null" in {
      val js = """{"in":null}"""
      read[OptionOption](js) must_== OptionOption(None)
    }

    "read an option inside an option for a value" in {
      val js = """{"in":1}"""
      read[OptionOption](js) must_== OptionOption(Some(Some(1)))
    }

    object ImplOverride {
      implicit object ImplOverrideReadable extends Consumer[ImplOverride] {
        def consume(obj: Ast.AstNode[_]): ImplOverride = ImplOverride(3854)
      }
    }
    case class ImplOverride(nr: Int)
    "resolve the custom implicit if one is provided" in {
      val js = """{"nr":3939}"""
      read[ImplOverride](js) must_== ImplOverride(3854)
    }
  }


//  "Macros.deserialize" should  {
//
//     "Build a list of maps" in {
//       val expected: List[Map[String, Int]] = Map("one" -> 1)::Map("two" -> 2)::Nil
//       val params: JValue = expected
//       deserialize[List[Map[String,Int]]](AstReader(params)) must_== expected
//     }
//
//     "Build maps of primitives with string key" in {
//       val expected = Map[String, Int](("a" -> 1), ("b" -> 2), ("c" -> 3))
//       val params: JValue = expected
//
//       deserialize[Map[String,Int]](AstReader(params)) must_== expected
//     }
//
//     "Build maps of primitives with Int key" in {
//       val expected = Map(1 -> 1, 2 -> 2, 100 -> 3)
//       val data: JValue = expected.map{case (k,v) => (k.toString, v)}
//
//       deserialize[Map[Int,Int]](AstReader(data)) must_== expected
//     }
//
//
//     "Build maps of Junks with string key" in {
//       val data: JValue = ("a" -> (("in1" -> 1) ~ ("in2" -> "aaa"))) ~
//         ("b" -> (("in1" -> 2) ~ ("in2" -> "bbb"))) ~
//         ("c" -> (("in1" -> 3) ~ ("in2" -> "ccc")))
//
//       val expected = Map("a"->Junk(1,"aaa"),"b"->Junk(2,"bbb"),"c"->Junk(3,"ccc"))
//
//       deserialize[Map[String,Junk]](AstReader(data)) must_== expected
//     }
//
//     "Build a map of objects with type parameters" in {
//       val data: JValue = ("a" -> ("in1" -> 2)) ~ ("b" -> ("in1" -> 3)) ~ ("c" -> ("in1" -> 4))
//       val expected = Map("a" -> WithTpeParams(2), "b" -> WithTpeParams(3), "c" -> WithTpeParams(4))
//       deserialize[Map[String,WithTpeParams[Int]]](AstReader(data)) must_== expected
//     }
//
//     "Generate a Junk" in {
//       deserialize[Junk](AstReader(refJunkDict)) must_== refJunk
//     }
//
//     "Generate a MutableJunk" in {
//       deserialize[MutableJunk](AstReader(refJunkDict)) must_== MutableJunk(2,"cats")
//     }
//
//     "Generate a ThingWithJunk" in {
//       val expected = ThingWithJunk("Bob", Junk(2, "SomeJunk..."))
//       val stuff =("name" -> "Bob") ~ ("junk" -> ("in1" -> 2) ~ ("in2" -> expected.junk.in2))
//       val result = deserialize[ThingWithJunk](AstReader(stuff))
//       result must_== expected
//     }
//
//     "Generate a MutableJunkWithJunk with provided Junk" in {
//       val result = MutableJunkWithJunk(1)
//       result.in2 = Junk(0, "cats")
//       val json:JObject = ("in1" -> 1) ~ ("in2" -> (("in1" -> 0) ~("in2" -> "cats")): JObject)
//       deserialize[MutableJunkWithJunk](AstReader(json)) must_== result
//     }
//
//     "Generate a MutableJunkWithJunk with missing Junk" in {
//       val result = MutableJunkWithJunk(1)
//       val json:JObject = ("in1" -> 1)
//       deserialize[MutableJunkWithJunk](AstReader(json)) must_== result
//     }
//
//     "Generate a MutableJunkWithField when field provided" in {
//       val expected = MutableJunkWithField(2)
//       expected.in2 = "cats"
//       deserialize[MutableJunkWithField](AstReader(refJunkDict)) must_== expected
//     }
//
//     "Generate a MutableJunkWithField when field missing" in {
//       val expected = MutableJunkWithField(2)
//       val params = JObject(("in1" -> JInt(2))::Nil)
//       deserialize[MutableJunkWithField](AstReader(params)) must_== expected
//     }
//
//     "Generate a class with var List when field missing" in {
//       class VarList { var lst: List[Int] = List.empty }
//       val expected = new VarList()
//       val params = JObject(Nil)
//       deserialize[VarList](AstReader(params)).lst must_== expected.lst
//     }
//
//     "Generate a class with var List when field present" in {
//       class VarList { var lst: List[Int] = List.empty }
//       val expected = new VarList()
//       expected.lst = List(1,2)
//       val params: JObject = ("lst" -> List(1,2))
//       deserialize[VarList](AstReader(params)).lst must_== expected.lst
//     }
//
//     "Generate a class with var Map when field missing" in {
//       class VarMap { var lst: Map[String, Int] = Map.empty }
//       val expected = new VarMap()
//       val params = JObject(Nil)
//       deserialize[VarMap](AstReader(params)).lst must_== expected.lst
//     }
//
//     "Generate a class with var Map when field present" in {
//       class VarMap { var lst: Map[String, Int] = Map.empty }
//       val expected = new VarMap()
//       expected.lst = Map("hi" -> 1)
//       val params = ("lst" -> Map("hi" -> 1))
//       deserialize[VarMap](AstReader(params)).lst must_== expected.lst
//     }
//
//     "Generate a 3 fold deap case class" in {
//       val expected = Crazy("crazyBob...",ThingWithJunk("Bob",Junk(2,"SomeJunk...")))
//       val stuff = ("name" -> expected.name) ~ ( "thg" ->
//         ( "name" -> expected.thg.name) ~ ( "junk" ->
//           ("in1" -> 2) ~ ("in2" -> expected.thg.junk.in2)
//           )
//       )
//
//       val result = deserialize[Crazy](AstReader(stuff))
//       result must_== expected
//     }
//
//     "Parse date info" in {
//       val expected = WithDate("Bob", new Date)
//       val params = ("name" -> expected.name) ~ ("date" -> defaultFormats.dateFormat.format(expected.date))
//
//       deserialize[WithDate](AstReader(params)) must_== expected
//     }
//
//     "Created ClassWithDef with param" in {
//       val params = ("in" -> 1)
//       deserialize[ClassWithDef](AstReader(params)) must_== (new ClassWithDef(1))
//     }
//
//     "Created ClassWithDef without param" in {
//       val params = JObject(Nil)
//       deserialize[ClassWithDef](AstReader(params)) must_== (new ClassWithDef)
//     }
//
//     "Generate a JunkWithDefault with a value" in {
//       var expected = JunkWithDefault(refJunk.in1,refJunk.in2)
//       deserialize[JunkWithDefault](AstReader(refJunkDict)) must_== expected
//     }
//
//     "Generate a JunkWithDefault without a value" in {
//       var expected = JunkWithDefault(refJunk.in1)
//       val params: JObject = ("in1" -> 2)
//       deserialize[JunkWithDefault](AstReader(params)) must_== expected
//     }
//
//     "Created ObjWithDefJunk without junk" in {
//       val expected = ObjWithDefJunk("Name")
//       val params: JObject = ("name" -> "Name")
//       deserialize[ObjWithDefJunk](AstReader(params)) must_== expected
//     }
//
//     "Created ObjWithDefJunk with provided junk" in {
//       val expected = ObjWithDefJunk("Name",Junk(2,"Provided"))
//       val params = ("name" -> "Name") ~ ("junk" ->
//           ("in1" -> 2) ~ ("in2" -> "Provided")
//         )
//       deserialize[ObjWithDefJunk](AstReader(params)) must_== expected
//     }
//
//     "Instance a case class with an Option" in {
//       val expected = WithOption(2,Some("Pizza pockets forever!"))
//       val params = ("in" -> expected.in) ~ ("opt" -> expected.opt.get)
//       deserialize[WithOption](AstReader(params)) must_== expected
//     }
//
//     "Instance a case class with a missing Option" in {
//       val expected = WithOption(2, None)
//       val params: JObject = ("in" -> expected.in)
//       deserialize[WithOption](AstReader(params)) must_== expected
//     }
//
//     "Generate a recursive Option" in {
//       val expected = OptionOption(Some(Some(5)))
//       val params: JObject = ("in" -> 5)
//       val result = deserialize[OptionOption](AstReader(params))
//       result must_== expected
//     }
//
//     "Handle type parameters" in {
//       val expected = WithTpeParams(100)
//       val params: JValue = ("in1" -> expected.in1)
//       deserialize[WithTpeParams[Int]](AstReader(params)) must_== expected
//     }
//
//     "Handle a tuple" in {
//       val expected = (2,3,"cats")
//       val params: JValue = ("_1" -> expected._1) ~ ("_2" -> expected._2) ~ ("_3" -> expected._3)
//       deserialize[(Int, Int, String)](AstReader(params)) must_== expected
//     }
//
//     "Handle nested type parameters, WithNstedTpeParams[U,U2](U, WithTpeParams[U2])" in {
//       val expected = new WithNstedTpeParams("cat",WithTpeParams(100))
//       val params: JValue = ("in1" -> expected.in1) ~ ("in2" -> ("in1" -> expected.in2.in1))
//       deserialize[WithNstedTpeParams[String, Int]](AstReader(params)) must_== expected
//     }
//
//     "Handle partially resolved, ResolvedParams[U](in3: U, in4:WithTpeParams[Int])" in {
//       val expected = new ResolvedParams("cat",WithTpeParams(100))
//       val params = ("in3" -> expected.in3) ~ ("in4" -> ("in1" -> expected.in4.in1))
//       deserialize[ResolvedParams[String]](AstReader(params)) must_== expected
//     }
//
//     "Curried case class" in {
//       val expected = Curried(1,2)(3)
//       val params = ("in1" -> expected.in1) ~ ("in2" -> expected.in2) ~ ("in3" -> 3)
//       deserialize[Curried](AstReader(params)) must_== expected
//     }
//
//     "parse List[Int]" in {
//       val expected = 1::2::3::4::Nil
//       val params: JValue = expected
//       val result = deserialize[List[Int]](AstReader(params))
//       result must_== expected
//     }
//
//     "parse List[WithTpeParams[String]]" in {
//       val expected = WithTpeParams("one")::WithTpeParams("two")::Nil
//       val params: JValue = List( ("in1" -> "one"), ("in1" -> "two"))
//       val result = deserialize[List[WithTpeParams[String]]](AstReader(params))
//       result must_== expected
//     }
//
//     "parse empty List[Int]" in {
//       val expected:List[Int] = Nil
//       val params: JValue = expected
//       val result = deserialize[List[Int]](AstReader(params))
//       result must_== expected
//     }
//
//     "parse List[List[Int]]" in {
//       val expected = (1::2::Nil)::(3::4::Nil)::Nil
//       val params: JValue = expected
//       val result = deserialize[List[List[Int]]](AstReader(params))
//
//       result must_== expected
//     }
//
//     "Parse WithList" in {
//       val expected = WithList("Bob", 1::4::Nil)
//       val params: JValue = ("name" -> "Bob") ~ ("lst" -> (1::4::Nil))
//       deserialize[WithList](AstReader(params)) must_== expected
//     }
//
//     "Parse WithSeq" in {
//       val expected = WithSeq(List(1,2,3))
//       val params: JValue = JObject(("in" -> JArray(expected.in.map(JInt(_)).toList))::Nil)
//       deserialize[WithSeq](AstReader(params)) must_== expected
//     }
//
//     "parse WithObjList" in {
//       val expected = WithObjList("Bob",ThingWithJunk("Bobby",Junk(1,"one"))::ThingWithJunk("Bill",Junk(2,"two"))::Nil)
//       val params: JValue = ("name" -> "Bob") ~ ("list" ->
//           ((("name" -> "Bobby") ~ ("junk" -> (("in1" -> 1)~("in2" -> "one"))))
//             ::(("name" -> "Bill") ~ ("junk" -> (("in1" -> 2)~("in2" -> "two"))))::Nil)
//         )
//       deserialize[WithObjList](AstReader(params)) must_== expected
//     }
//
//     "parse List[Bill]" in {
//       val expected = Bill(1)::Bill(3)::Nil
//       val params: JValue = List(("in" -> 1),("in" -> 3))
//       deserialize[List[Bill]](AstReader(params)) must_== expected
//     }
//
//     "parse BillyB which extends Billy[Int]" in {
//       val expected = BillyB(3)
//       val params: JValue = ("in" -> 3)
//       deserialize[BillyB](AstReader(params)) must_== expected
//     }
//     "Throw ParseException with a bad map value for 'in'" in {
//       val params: JValue = ("in1" -> "2ffds") ~ ("in2" -> "cats")
//       deserialize[Junk](AstReader(params)) must throwA[MappingException]
//     }
//
//     // TODO: How to get default args for alternate constructors
//     class Multi(val in1: Int, val in2: String) {
//       def this(in3: Int, in4: String, in5: String) = this(in3, in4+in5)
//       def this(in6: Int, in7: String, in8: String, in9: String = "baz") = this(in6, in7, in8+in9)
//       override def equals(other: Any) = other match {
//         case m: Multi if(m.in1 == in1 && m.in2 == in2) => true
//         case _ => false
//       }
//     }
//
//     "Deserialize a class with multiple constructors" in {
//       val params1: JValue = ("in1" -> 1) ~ ("in2" -> "str")
//       val params2: JValue = ("in3" -> 3) ~ ("in4" -> "foo") ~ ("in5" -> "bar")
//       val params3: JValue = ("in6" -> 6) ~ ("in7" -> "foo") ~ ("in8" -> "bar") ~ ("in9" -> "baz")
//       deserialize[Multi](AstReader(params1)) must_== new Multi(1, "str")
//       deserialize[Multi](AstReader(params2)) must_== new Multi(3, "foobar")
//       deserialize[Multi](AstReader(params3)) must_== new Multi(6, "foobarbaz")
//     }
//
//     class WithSetters {
//       private var i = 0
//       def getI = i
//       def setI(in: Int) { i = in }
//       override def equals(other: Any) = other match {
//         case that: WithSetters => that.getI == i
//         case _ => false
//       }
//     }
//     "Tester should work" in {
//       val params: JValue = ("i" -> 1)
//       val result = new WithSetters()
//       result.setI(1)
//       deserialize[WithSetters](AstReader(params)) must_== result
//     }
//
//     "Deserialize a Object with a type alias" in {
//       val expected: Map[String, Any] = Map("in" -> Map("in1" -> 1, "in2" -> "2"))
//       val params = JObject(List("in" -> JObject(List("in1" -> JInt(1), "in2" -> JString("2")))))
//       deserialize[WithAlias](AstReader(params)) must_== WithAlias(new Foo(1,"2"))
//     }
//   }
//
//   "Macro.extract" should {
//     "parse List[Bill]" in {
//       val expected = Bill(1)::Bill(3)::Nil
//       val params: JValue = List(("in" -> 1),("in" -> 3))
//       extract[List[Bill]](params) must_== expected
//     }
//   }
}