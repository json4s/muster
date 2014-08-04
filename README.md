# Muster

[![Build Status](https://travis-ci.org/json4s/muster.svg?branch=master)](https://travis-ci.org/json4s/muster)

A library for macro based serializers to different formats.
It uses scala macros so no reflection is involved and it will generate code at compile time
that kind of looks like it would have been handwritten.  It is written with the idea of extension, so it's easy to
add your own formats.

You can find the [documentation on the site](http://muster.json4s.org/docs)

## Getting the library

This only works with scala 2.11.
The library is published to maven central so you can get it with:

```scala
libraryDependencies += "org.json4s" %% "muster-codec-jawn" % "0.2.0"
libraryDependencies += "org.json4s" %% "muster-codec-jackson" % "0.2.0"
```

Bring your own AST (BYA):

For Json4s: 

```scala
libraryDependencies += "org.json4s" %% "muster-codec-json4s" % "0.2.0"
```

For Play Json:

```scala
libraryDependencies += "org.json4s" %% "muster-codec-play-json" % "0.3.0-SNAPSHOT" // Not yet published
```

Prettier case class for debugging:

```scala
libraryDependencies += "org.json4s" %% "muster-codec-string" % "0.2.0"
```

## How does it work?

You can find the [documentation on the site](http://muster.json4s.org/docs)
The idea is that things work a little bit like this:

### Writing

```scala
case class Person(id: Long, name: String, age: Int)
val person = Person(1, "Luke", 38)

import muster.codec.jawn._
JsonFormat.from(person)
JsonFormat.into(new File("luke.json")).from(person)
JsonFormat.Pretty.from(person)

import muster.codec.jawn.api._
person.asJson // calls: JsonFormat.from(person) and produces {"id":1,"name":"Luke","age":38}
person.asPrettyJson /* calls: JsonFormat.Pretty.from(person) and produces
                       {
                         "id":1,
                         "name":
                         "Luke",
                         "age":38
                       } */


import muster.codec.json4s._
// decompose to a Json4s AST
Json4sFormat.from(person)

import muster.codec.json4s.api._
person.asJValue


// Serialize Json4s AST's
import muster.codec.json4s.api._
JsonFormat.into(new File("jvalues.json")).from(person.asJValue)
JsonFormat.Pretty.from(person.asJValue)

// Prettier string formatting of case classes
import muster.codec.string._
StringFormat.from(person)

import muster.codec.string.api._
person.asString 
// calls muster.codec.string.api.StringFormat.from(person) 
// and produces Person(id: 1, name: "Luke", age: 38)


```

### Reading

Similarly reading can be achieved with

```scala
// Extract a person from a json stream
import muster.codec.jawn._
JsonFormat.as[Person](/* file | string | reader | byte array | input stream | URL */ input)

// Extract a person from a Json4s AST
import muster.codec.json4s._
JValueFormat.as[Person](personJValue)

// Parse a source to a Json4s AST
import muster.codec.json4s._
JsonFormat.as[JValue](/* file | string | reader | byte array | input stream | URL */ input)
```

## What's inside?

Seamless integration with the Json4s AST, it can be used to extract objects from and decompose objects to json4s AST's.
It can be used to parse Json4s AST's 

Currently muster supports JSON through parsing with jackson or jawn and it can extract the following things:

Object mapping features:

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
* Allows choosing between different option treatments for formats that support omission instead of null

Possibly added next:

* Support for scala enums
* Support for java enums
* Support for renaming fields 
* Support for using a map as an input source
* Support for serializing and deserializing from mongodb
* Provide a way to use custom producibles
* Provide a way to use custom consumables
* Support for common annotations like @JsonProperty and so on to provide overrides for behavior

