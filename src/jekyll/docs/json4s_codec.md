---
layout: default
---
# Json4s Codec

Muster has an integration with json4s for parsing json. You can get muster-codec-json4s from maven central. Check the [releases page](https://github.com/json4s/muster/releases) for the latest version.

```scala
libraryDependencies += "org.json4s" %% "muster-codec-json" % "latest" // Comes as a transitive dependency
libraryDependencies += "org.json4s" %% "muster-codec-json4s" % "latest"
```

In the examples we'll use this case class

```scala
case class Person(id: Long, name: String, age: Int)
val person = Person(1, "Luke", 38)
val personJValue: JValue = JObject("id" -> JInt(1) :: "name" -> JString("Luke") :: "age" -> JInt(38) :: Nil)
```

### Deserializing

To extract an object from a json ast you make use of the Json4sCodec.

```scala
import muster._
import muster.codec.json4s._

Json4sCodec.as[Person](personJValue)
```

You can also make use of a parser and generate `JValues`

For example extracting a JValue with the jawn parser

```scala
import muster._
import muster.codec.json4s._
import muster.codec.jawn._
JawnCodec.as[JValue]("""{"id":1,"name":"Luke","age":38}""")
JawnCodec.as[JValue](new URL("http://somewhere.com/luke.json"))
```

The example shows getting json from a file, a string and a url. You can also provide it with a byte array or an input stream.

### Serializing

To serialize an object to json you also use the Json4sCodec.

```scala
import muster._
import muster.codec.json4s._

Json4sCodec.from(person)
```

The example above produces

```scala
JObject("id" -> JInt(1) :: "name" -> JString("Luke") :: "age" -> JInt(38) :: Nil)
```

You don't have to serialize to strings you can also serialize to files etc.

```scala
import muster._
import muster.codec.json4s._

Json4sCodec.into(new File("luke.json")).from(personJValue)
```

The example above produces a file with the json for the person.
There is a little bit of optional syntactic sugar the comes with the json4s integration

```scala
import muster._
import muster.codec.json4s._
import muster.codec.json4s.api._

person.asJValue
```
The example above calls: `Json4sCodec.from(person)` and produces 

```scala
JObject("id" -> JInt(1) :: "name" -> JString("Luke") :: "age" -> JInt(38) :: Nil)
```




