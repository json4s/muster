muster
======

A library for macro based serializers to many different formats.
It uses scala macros so no reflection is involved and it will generate code at compile time
that kind of looks like it would have been handwritten.  It is written with the idea of extension, so it's easy to
add your own formats.

The idea is that things work a little bit like this

```scala
case class Person(id: Long, name: String, age: Int)
val person = Person(1, "Luke", 38)
person.writeFormatted(Muster.into.ByteString)
person.writeFormatted(Muster.into.ByteBuffer)
person.writeFormatted(Muster.into.ShowFormat)
person.writeFormatted(Muster.into.CompactJsonString)
person.writeFormatted(Muster.into.PrettyJsonString)
person.writeFormatted(Muster.into.Protobuf[Protocol.Person])
```


Similarly reading can be achieved with

```scala
Muster.readFormatted[Person](Muster.from.Json)
person.writeFormatted[Person](Muster.from.Protobuf[Protocol.Person])
Muster.readFormatted[Person](Muster.from.ByteString)
Muster.readFormatted[Person](Muster.from.ByteBuffer)
```