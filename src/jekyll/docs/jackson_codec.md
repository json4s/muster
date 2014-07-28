---
layout: default
---
## Jackson Codec

Muster has an integration with jackson for parsing json. You can get muster-codec-jackson from maven central. Check the [releases page](https://github.com/json4s/muster/releases) for the latest version.

```scala
libraryDependencies += "org.json4s" %% "muster-codec-json" % "latest" // Comes as a transitive dependency
libraryDependencies += "org.json4s" %% "muster-codec-jackson" % "latest"
```

In the examples we'll use this case class

```scala
case class Person(id: Long, name: String, age: Int)
val person = Person(1, "Luke", 38)
```

### Deserializing

To extract an object from a json stream you make use of the JacksonCodec.

```scala
import muster._
import muster.codec.jackson._

JacksonCodec.as[Person](new File("luke.json"))
JacksonCodec.as[Person]("""{"id":1,"name":"Luke","age":38}""")
JacksonCodec.as[Person](new URL("http://somewhere.com/luke.json"))
```

The example shows getting json from a file, a string and a url. You can also provide it with a byte array or an input stream.

### Serializing

To serialize an object to json you also use the JacksonCodec.

```scala
import muster._
import muster.codec.jackson._

JacksonCodec.from(person)
```

The example above produces

```json
{"id":1,"name":"Luke","age":38}
```

```scala
import muster._
import muster.codec.jackson._

JacksonCodec.Pretty.from(person)
```

The example above produces

```json
{
  "id":1,
  "name": "Luke",
  "age":38
}
```

You don't have to serialize to strings you can also serialize to files etc.

```scala
import muster._
import muster.codec.jackson._

JacksonCodec.into(new File("luke.json")).from(person)
```

The example above produces a file with the json for the person.
There is a little bit of optional syntactic sugar the comes with the jackson integration

```scala
import muster._
import muster.codec.jackson._
import muster.codec.jackson.api._

person.asJson
```
The example above calls: `JacksonCodec.from(person)` and produces 

```json
{"id":1,"name":"Luke","age":38}
```

To make it output pretty printed json:

```scala
import muster._
import muster.codec.jackson._
import muster.codec.jackson.api._

person.asPrettyJson 
```

The example above calls: JacksonCodec.Pretty.from(person) and produces

```json
{
  "id":1,
  "name": "Luke",
  "age":38
}
```