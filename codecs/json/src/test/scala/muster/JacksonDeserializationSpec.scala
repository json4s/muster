package muster

import org.specs2.mutable.Specification
import java.util.{TimeZone, Date}
import java.util
import scala.Some
import org.json4s.DefaultFormats

//import org.joda.time.DateTime
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
  val format = JsonTestFormat

  val refJunk = Junk(2, "cats")
  val refJunkDict: String = org.json4s.jackson.Serialization.write(refJunk)

  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  type Foo = Junk

  case class WithAlias(in: Foo)


  def read[T](value: String)(implicit cons: Consumer[T]) = format.as[T](value)(cons)

  "Muster.consume.Json" should {
    //    "read a dateTime" in {
    //      val date = DateTime.now
    //      val ds = Muster.from.JsonString.dateFormat.print(new DateTime(date))
    //      read[DateTime]("\""+ds+"\"") must_== Muster.from.JsonString.dateFormat.parseDateTime(ds)
    //    }
    "read a date" in {
      val date = new Date()
      val ds = SafeSimpleDateFormat.Iso8601Formatter.format(date)
      read[Date]("\"" + ds + "\"") must_== SafeSimpleDateFormat.Iso8601Formatter.parse(ds)
    }

    "read a symbol" in {
      val sym = 'a_symbol_of_sorts
      val js = "\"" + sym.name + "\""
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
      read[Map[String, List[Option[Int]]]](json) must_== Map("one" -> List(Some(1)), "two" -> List(Some(3), None, Some(4)), "three" -> List(Some(394)))
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
      read[WithList](js) must_== WithList(List(1, 2, 3))
    }

    "read an object with list and map" in {
      val js = """{"lst":[1,2,3], "map":{"foo":1,"bar":2}}"""
      read[ObjWithListMap](js) must_== ObjWithListMap(List(1, 2, 3), Map("foo" -> 1, "bar" -> 2))
    }

    "read an object with a date" in {
      val date = new Date
      val ds = SafeSimpleDateFormat.Iso8601Formatter.format(date)
      val pd = SafeSimpleDateFormat.Iso8601Formatter.parse(ds)
      val js = s"""{"date":"$ds"}"""
      read[WithDate](js) must_== WithDate(pd)
    }
//
//    "read an object with a datetime" in {
//      val date = DateTime.now.toDate
//      val ds = SafeSimpleDateFormat.Iso8601Formatter.format(date)
//      val pd = SafeSimpleDateFormat.Iso8601Formatter.parse(ds)
//      val js = s"""{"date":"$ds"}"""
//      read[WithDateTime](js) must_== WithDateTime(new DateTime(pd))
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
    val thingWithJunkJson = s"""{"name":"foo","junk":$junkJson}"""
    val junk = Junk(123, "456")
    val thingWithJunk = ThingWithJunk("foo", junk)
    "read a ThingWithJunk" in {
      read[ThingWithJunk](thingWithJunkJson) must_== thingWithJunk
    }

    "read type aliased thing with junk when alias is defined in a package object" in {
      read[aliasing.WithAlias]( s"""{"in":{"in1":123,"in2":"456"}}""") must_== aliasing.WithAlias(junk)
    }

    "read type aliased thing with junk when alias is defined in an object" in {
      read[Aliased.WithAlias]( s"""{"in":{"in1":123,"in2":"456"}}""") must_== Aliased.WithAlias(junk)
    }

    "read type aliased thing with junk when alias is defined in this class" in {
      read[this.WithAlias]( s"""{"in":{"in1":123,"in2":"456"}}""") must_== this.WithAlias(junk)
    }

    "read type aliased thing with junk when alias is defined in another class and companion object is used to invoke the macro" in {
      val ac = new Ac
      read[ac.WithAlias]( s"""{"in":{"in1":123,"in2":"456"}}""") must_== ac.WithAlias(junk)
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

    "read a class with java style getter/setter definitions" in {
      val js = """{"id":1,"name":"Tom"}"""
      val result = read[JavaStyle](js)
      result.getId must_== 1
      result.getName must_== "Tom"
    }

    "read a java class" in {
      val js = """{"id":1,"name":"Tom"}"""
      val result = read[SimpleJava](js)
      result.getId must_== 1
      result.getName must_== "Tom"
    }

    "read a java class with a list and name property" in {
      val js = """{"name":"a thing","lst":[1,2,3,4]}"""
      val result = read[JavaListAndName](js)
      result.getName must_== "a thing"
      val lst = new util.ArrayList[Integer]
      lst.add(1)
      lst.add(2)
      lst.add(3)
      lst.add(4)
      result.getLst must_== lst
    }

    "read a java class with a set and name property" in {
      val js = """{"name":"a thing","set":[1,2,3,4]}"""
      val result = read[JavaSetAndName](js)
      result.getName must_== "a thing"
      val lst = new util.HashSet[Integer]
      lst.add(1)
      lst.add(2)
      lst.add(3)
      lst.add(4)
      result.getSet must_== lst
    }

    "read a java class with a map and name property" in {
      val js = """{"name":"a thing","dict":{"one":1,"two":2}}"""
      val result = read[JavaMapAndName](js)
      result.getName must_== "a thing"
      val dict = new util.HashMap[String, Int]()
      dict.put("one", 1)
      dict.put("two", 2)
      result.getDict must_== dict
    }

    "read a java class with a list of map of list" in {
      val js = """{"name":"a thing", "lst":[{"one":[1,2,3], "two":[4,5,6]}]}"""
      val result = read[JavaListOfMapOfList](js)
      result.getName must_== "a thing"
      val dict = new util.HashMap[String, util.List[Integer]]()
      val lst1 = new util.ArrayList[Integer]()
      lst1.add(1)
      lst1.add(2)
      lst1.add(3)
      val lst2 = new util.ArrayList[Integer]()
      lst2.add(4)
      lst2.add(5)
      lst2.add(6)
      dict.put("one", lst1)
      dict.put("two", lst2)
      val lst = new util.ArrayList[util.HashMap[String, util.List[Integer]]]()
      lst.add(dict)
      result.getLst must_== lst
    }

    "reads mutable junk" in {
      val js = """{"in1":2334, "in2": "some string"}"""
      read[MutableJunk](js) must_== MutableJunk(2334, "some string")
    }

    "reads mutable junk with a field" in {
      val js = """{"in1":2334, "in2": "some string"}"""
      val expected = MutableJunkWithField(2334)
      expected.in2 = "some string"
      val result = read[MutableJunkWithField](js)
      result.in1 must_== expected.in1
      result.in2 must_== expected.in2
    }

    "read mutable junk with a junk field" in {
      val js = s"""{"in1":3994, "in2":$junkJson}"""
      val expected = MutableJunkWithJunk(3994)
      expected.in2 = junk
      val result = read[MutableJunkWithJunk](js)
      result.in1 must_== expected.in1
      result.in2 must_== expected.in2
    }

    "read a class with a default value and data is in the json" in {
      val js = """{"in1": 123, "in2": "not the default" }"""
      read[JunkWithDefault](js) must_== JunkWithDefault(123, "not the default")
    }

    "read a class with a default value and use the default value for a missing json value" in {
      val js = """{"in1": 123}"""
      read[JunkWithDefault](js) must_== JunkWithDefault(123)
    }

    "read a class with a list and name property" in {
      val js = """{"name":"list and name","lst":[3949,3912,2050]}"""
      read[WithListAndName](js) must_== WithListAndName("list and name", List(3949,3912,2050))
    }

    "read a class with an object list and a name" in {
      val js = s"""{"name":"list and name","list":[$thingWithJunkJson]}"""
      read[WithObjList](js) must_== WithObjList("list and name", List(thingWithJunk))
    }

    "read a curried object" in {
      val js = """{"in1":1395,"in2":4059,"in3":395}"""
      read[Curried](js) must_== Curried(1395, 4059)(395)
    }

    "read an object with a type param" in {
      val js = """{"in1":39589}"""
      read[WithTpeParams[Int]](js) must_== WithTpeParams(39589)
    }

    "read an object with a seq" in {
      read[WithSeq]("""{"in":[1,3,4]}""") must_== WithSeq(Seq(1,3,4))
    }

    "read an object with nested type params" in {
      val js = """{"in1":12,"in2":{"in1":94}}"""
      read[WithNstedTpeParams[Int, Int]](js) must_== new WithNstedTpeParams(12, WithTpeParams(94))
    }

    "read an object with nested type params and a resolved param" in {
      val js = """{"in3":12,"in4":{"in1":94}}"""
      read[ResolvedParams[Int]](js) must_== ResolvedParams(12, WithTpeParams(94))
    }

    "read an object with date and a name" in {
      val date = new Date
      val ds = SafeSimpleDateFormat.Iso8601Formatter.format(date)
      val pd = SafeSimpleDateFormat.Iso8601Formatter.parse(ds)
      val js = s"""{"date":"$ds","name":"with date and name"}"""
      read[WithDateAndName](js) must_== new WithDateAndName("with date and name",pd)
    }

    "read a regular class with a val definition and use the data from json" in {
      val js = """{"in":49}"""
      read[ClassWithDef](js) must_== new ClassWithDef(49)
    }

    "read a regular class with a val definition and use the default value" in {
      val js = """{}"""
      read[ClassWithDef](js) must_== new ClassWithDef()
    }

    "read an object with a default complex object" in {
      val js = s"""{"name":"junk with default", "junk":$junkJson}"""
      read[ObjWithDefJunk](js) must_== ObjWithDefJunk("junk with default", junk)
    }

    "read an object with a default complex object and use the default value" in {
      val js = s"""{"name":"junk with default"}"""
      read[ObjWithDefJunk](js) must_== ObjWithDefJunk("junk with default")
    }

    /*

    class Billy[U](in: U)

    case class BillyB(in: Int) extends Billy[Int](in)
     */


  }


}