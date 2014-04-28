# muster

A library for macro based serializers to many different formats.
It uses scala macros so no reflection is involved and it will generate code at compile time
that kind of looks like it would have been handwritten.  It is written with the idea of extension, so it's easy to
add your own formats.

The idea is that things work a little bit like this

## Writing

```scala
case class Person(id: Long, name: String, age: Int)
val person = Person(1, "Luke", 38)

import muster.codec._
import json.api._
JsonFormat.from(person)
JsonFormat.into(new File("luke.json")).from(person)
JsonFormat.Pretty.from(person)
person.asJson // calls: JsonFormat.from(person) and produces {"id":1,"name":"Luke","age":38}
person.asPrettyJson /* calls: JsonFormat.Pretty.from(person) and produces
                       {
                         "id":1,
                         "name":
                         "Luke",
                         "age":38
                       } */


import string.api._
StringFormat.from(person)
person.asString // calls: muster.codec.string.api.StringFormat.from(person) and produces Person(id: 1, name: "Luke", age: 38)

/*
Not Yet Implemented:
Muster.produce.ByteBuffer.from(person)
Muster.produce.ByteString.from(person)
Muster.produce.Protobuf[Protocol.Person].from(person)
*/

```

## Reading

Similarly reading can be achieved with

```scala
import muster.codec.json.api._
JsonFormat.as[Person](/* file | string | reader | byte array | input stream | URL */ input)
/*
Not Yet Implemented:
Muster.consume.Protobuf[Protocol.Person].as[Person](/* file | string | reader | byte array | input stream | URL */ input)
Muster.consume.ByteString.as[Person](/* file | string | reader | byte array | input stream | URL */ input)
Muster.consume.ByteBuffer.as[Person](/* file | string | reader | byte array | input stream | URL */ input)
*/
```

## What's inside

Currently muster supports JSON through parsing with jackson and it can extract the following things:
* Primitive values like String, Int, Date
* All scala collections
* Scala maps with string keys
* Java collections like java.util.List and java.util.Set
* Java maps (that implement java.util.Map)
* Java classes with bean getter/setters
* Scala classes with public vars
* Scala classes with java style getter/setter methods
* Scala case classes
* Classes initialized through a constructor only
* Classes with type parameters
* Support for maps with different keys than String
* Polymorphic classes/collections through a configurable type hint field and strategy


Expected to be added next:
* Support for using a map as an input source
* Support for serializing and deserializing from mongodb
* Support for writing bytebuffers (through the pickling format)
* Support for writing akka.util.ByteString (through the pickling format)
* Support for streaming large collections through a scala iterator, java iterator or scala stream
