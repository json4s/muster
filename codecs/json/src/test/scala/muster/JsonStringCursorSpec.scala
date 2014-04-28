//package muster
//
//import org.specs2.mutable.Specification
//
//class JsonStringCursorSpec extends Specification {
//
//  val json =
//      """{ "cats": 34, "dogs": { "cats": true, "fish": [1, "cats", [1, 2, 3]] } }"""
//
//  val verySimpleJson =
//      """{ "cats": 34 }"""
//
//  val intArrayJson = """[1, 2, 3]"""
//
//  val bigger =
//    """
//      |{
//      |  "id": 0,
//      |  "guid": "d117fddc-0104-441b-96c9-a32fd5609445",
//      |  "isActive": false,
//      |  "balance": "$3,657.00",
//      |  "picture": "http://placehold.it/32x32",
//      |  "age": 29,
//      |  "name": "Goodman Cook",
//      |  "gender": "male",
//      |  "company": "Artworlds",
//      |  "email": "goodmancook@artworlds.com",
//      |  "phone": "+1 (805) 577-2238",
//      |  "address": "710 Vanderbilt Avenue, Strykersville, Oregon, 7626",
//      |  "about": "Voluptate nisi et aliquip fugiat tempor exercitation proident ullamco anim cillum nulla adipisicing laboris sit. Ex ullamco nulla occaecat dolor. Reprehenderit ut incididunt do duis.\r\n",
//      |  "registered": "1989-06-20T21:27:29 +07:00",
//      |  "latitude": 61.896839,
//      |  "longitude": 126.790112,
//      |  "tags": [
//      |    "occaecat",
//      |    "veniam",
//      |    "non",
//      |    "incididunt",
//      |    "sint",
//      |    "aliqua",
//      |    "anim"
//      |  ],
//      |  "friends": [
//      |    {
//      |      "id": 0,
//      |      "name": "Malone Turner"
//      |    },
//      |    {
//      |      "id": 1,
//      |      "name": "Krystal Morrow"
//      |    },
//      |    {
//      |      "id": 2,
//      |      "name": "Jordan Ashley"
//      |    }
//      |  ],
//      |  "randomArrayItem": "lemon"
//      |}""".stripMargin
//
//  "A JsonStringCursor" should {
//    "read an empty object" in {
//      val r1 = Json.createCursor("{}")
//      val abj = r1.nextNode()
//      abj must beAnInstanceOf[JsonInputCursor.JsonObjectNode]
//      r1.hasRemaining must beFalse
//    }
//
//    "read an empty array" in {
//      val r1 = Json.createCursor("[]")
//      val abj = r1.nextNode()
//      abj must beAnInstanceOf[JsonInputCursor.JsonArrayNode]
//      r1.hasRemaining must beFalse
//    }
//
//    "parse a single string value" in {
//      val r1 = Json.createCursor("\"foo\"")
//      r1.nextNode() must_== Ast.TextNode("foo")
//      r1.hasRemaining must beFalse
//    }
//
//    "parse a single int value" in {
//      val r1 = Json.createCursor("1234")
//      r1.nextNode() must_== Ast.NumberNode("1234")
//      r1.hasRemaining must beFalse
//    }
//
//    "parse a single boolean value" in {
//      val r1 = Json.createCursor("true")
//      r1.nextNode() must_== Ast.TrueNode
//      r1.hasRemaining must beFalse
//      val r2 = Json.createCursor("false")
//      r2.nextNode() must_== Ast.FalseNode
//      r2.hasRemaining must beFalse
//
//    }
//
//    "parse a very simple json object" in {
//      val r1 = Json.createCursor(verySimpleJson)
//      r1.nextNode() must beAnInstanceOf[JsonInputCursor.JsonObjectNode]
//      r1.hasRemaining must beFalse
//    }
//
//    "parse an array of ints" in {
//      val r1 = Json.createCursor(intArrayJson)
//      val obj = r1.nextNode()
//      obj must beAnInstanceOf[JsonInputCursor.JsonArrayNode]
//      r1.hasRemaining must beFalse
//    }
//
//    "parse a json object" in {
//      val r1 = Json.createCursor(json)
//      val obj = r1.nextNode()
//      obj must beAnInstanceOf[JsonInputCursor.JsonObjectNode]
//      r1.hasRemaining must beFalse
//    }
//
//    "parse a bigger json object" in {
//      val r1 = Json.createCursor(bigger)
//      val obj = r1.nextNode()
//      obj must beAnInstanceOf[JsonInputCursor.JsonObjectNode]
//      r1.hasRemaining must beFalse
//    }
//
//    "find the next string" in {
//      val cursor = Json.createCursor(""""Hello world" """)
//      cursor.readString() must_== Ast.TextNode("Hello world")
//      cursor.hasRemaining must beTrue
//    }
//    "find the next string with escaped escape" in {
//      val cursor = Json.createCursor(""""Hello world\\" """)
//      cursor.readString() must_== Ast.TextNode("Hello world\\")
//      cursor.hasRemaining must beTrue
//    }
//    "unescape string properly" in {
//      val r = Json.createCursor("\"abc\\\"\\\\\\/\\b\\f\\n\\r\\t\\u00a0\"").readString()
//      r must_== Ast.TextNode("abc\"\\/\b\f\n\r\t\u00a0")
//    }
//
//    "strip down a string" in {
//      val r = Json.createCursor("\"Hello world! \\\" this is cool \\\" \" ").readString()
//      r must_== Ast.TextNode("Hello world! \" this is cool \" ")
//    }
//
//    "strip down a number" in {
//      val r1 = Json.createCursor("34 }")
//      r1.readNumber() must_== Ast.NumberNode("34")
//      r1.hasRemaining must beTrue
//
//      val r2 = Json.createCursor("34, ")
//      r2.readNumber() must_== Ast.NumberNode("34")
//      r2.hasRemaining must beTrue
//
//      val r3 = Json.createCursor("34.54, ")
//      r3.readNumber() must_== Ast.NumberNode("34.54")
//      r3.hasRemaining must beTrue
//
//      val r4 = Json.createCursor("-34e-5, ")
//      r4.readNumber() must_== Ast.NumberNode("-34e-5")
//      r4.hasRemaining must beTrue
//    }
//
//    "strip down a boolean" in {
//      val r1 = Json.createCursor("true, ")
//      r1.readBoolean() must_== Ast.TrueNode
//      r1.hasRemaining must beTrue
//      val r2 = Json.createCursor("false, ")
//      r2.readBoolean() must_== Ast.FalseNode
//      r2.hasRemaining must beTrue
//    }
//
//    "break down an object" in {
//      val obj = Json.createCursor(json).readObject()
//
//      obj.readNumberField("cats").value must_== "34"
//      val obj2 = obj.readObjectField("dogs")
//      obj2.readBooleanField("cats") must_== Ast.TrueNode
//      val arr = obj2.readArrayField("fish")
//      arr.readNumber().toInt must_== 1
//      arr.readString().value must_== "cats"
//      val arr2 = arr.readArray()
//      arr2.readNumber().toInt must_== 1
//      arr2.readNumber().toInt must_== 2
//      arr2.readNumber().toInt must_== 3
//
//      val wobj = Json.createCursor("""{ "ca[]ts": true, "fi{sh": [1, "cats", [1, 2, 3]] }""").readObject()
//      wobj.readBooleanField("ca[]ts") must_== Ast.TrueNode
//      val arr3 = wobj.readArrayField("fi{sh")
//      arr3.readNumber().toInt must_== 1
//      arr3.readString().value must_== "cats"
//      val arr4 = arr3.readArray()
//      arr4.readNumber().toInt must_== 1
//      arr4.readNumber().toInt must_== 2
//      arr4.readNumber().toInt must_== 3
//    }
//
//    "break down an array" in {
//      val reader = Json.createCursor("""[ 3, false, { "cat": "cool" }, [ 1, 2]] """).readArray()
//      reader.readInt().value must_== 3
//      reader.readBoolean().value must beFalse
//      val obj = reader.readObject()
//      obj.readStringField("cat").value must_== "cool"
//      val arr2 = reader.readArray()
//      arr2.readInt() .value must_== 1
//      arr2.readInt() .value must_== 2
//    }
//
//    "Throw a ParseException on wrong type of json" in {
//      val json =  """[{"one": 1, "two": true}, "one": 11, "two": false}]"""
//      Json.createCursor(json).readObject() must throwA[ParseException]
//    }
//
//
//    "Throw a ParseException on bad json" in {
//      val json =  """{"one": 1, "two": gtrue}"""
//      Json.createCursor(json).readObject() must throwA[ParseException]
//    }
//  }
//}