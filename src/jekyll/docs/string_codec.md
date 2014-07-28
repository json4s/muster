---
layout: default
---
# String Codec

Muster allows for printing case classes with their field labels. You can get muster-codec-string from maven central. Check the [releases page](https://github.com/json4s/muster/releases) for the latest version.

```scala
libraryDependencies += "org.json4s" %% "muster-codec-json" % "latest" // Comes as a transitive dependency
libraryDependencies += "org.json4s" %% "muster-codec-string" % "latest"
```

In the examples we'll use this case class

```scala
case class Person(id: Long, name: String, age: Int)
val person = Person(1, "Luke", 38)
```

### Printing a string

To extract an object from a json ast you make use of the Json4sCodec.

```scala
import muster._
import muster.codec.string._

StringFormat.from(person)
```

There is a bit of syntactic sugar that you can add.

```scala
import muster._
import muster.codec.string._
import muster.codec.string.api._

person.asString
```

Both of these examples produce

```
Person(id: 1, name: "Luke", age: 38)
```