---
layout: default
---
# Argonaut Codec

Muster has an integration with argonaut for parsing json. You can get muster-codec-argonaut from maven central. Check the [releases page](https://github.com/json4s/muster/releases) for the latest version.

```scala
libraryDependencies += "org.json4s" %% "muster-codec-json" % "latest" // Comes as a transitive dependency
libraryDependencies += "org.json4s" %% "muster-codec-argonaut" % "latest"
```

In the examples we'll use this case class

```scala
case class Person(id: Long, name: String, age: Int)
val person = Person(1, "Luke", 38)
val personJson: Json = JsObject(Seq("id" -> JsNumber(1), "name" -> JsString("Luke"), "age" -> JsNumber(38)))
```

### Deserializing

To extract an object from a json ast you make use of the ArgonautCodec.

```scala
import muster._
import muster.codec.argonaut._

ArgonautCodec.as[Person](personJson)
```

You can also make use of a parser and generate `JsValues`

For example extracting a Json value with the jawn parser

```scala
import muster._
import muster.codec.argonaut._
import muster.codec.jawn._
JawnCodec.as[Json]("""{"id":1,"name":"Luke","age":38}""")
JawnCodec.as[Json](new URL("http://somewhere.com/luke.json"))
```

The example shows getting json from a file, a string and a url. You can also provide it with a byte array or an input stream.

### Serializing

To serialize an object to json you also use the ArgonautCodec.

```scala
import muster._
import muster.codec.argonaut._

ArgonautCodec.from(person)
```

The example above produces

```scala
JsObject(Seq("id" -> JsNumber(1), "name" -> JsString("Luke"), "age" -> JsNumber(38)))
```

You don't have to serialize to strings you can also serialize to files etc.

```scala
import muster._
import muster.codec.argonaut._

ArgonautCodec.into(new File("luke.json")).from(personJsValue)
```

The example above produces a file with the json for the person.
There is a little bit of optional syntactic sugar the comes with the argonaut integration

```scala
import muster._
import muster.codec.argonaut._
import muster.codec.argonaut.api._

person.asJsValue
```

The example above calls: `ArgonautCodec.from(person)` and produces 

```scala
JsObject(Seq("id" -> JsNumber(1), "name" -> JsString("Luke"), "age" -> JsNumber(38)))
```




