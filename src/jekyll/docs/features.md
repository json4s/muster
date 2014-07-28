---
layout: default
---
# Features

The most basic functionality muster provides is printing your case classes with labels for the fields in addition to the values.

It also provides integration with several json parsers. These parsers can be used to serialize and deserialize classes. 

### Supported parsers

* [Jawn](jawn_codec.html)
* [Jackson](jackson_codec.html)

In addition to parsing json it provides integration with several json asts, and can be used to parse streams, strings etc to an AST. The AST support also allows for rendering ASTs to streams.  Besides the rendering and parsing muster also knows how to compose and decompose objects from json ASTs. 

### Supported ASTs

* [Json4s](json4s_codec.html)

### Object mapping features

* Primitive values like String, Int, Date
* All scala collections
* Scala maps, with configurable key serializers
* Java collections like java.util.List and java.util.Set
* Java maps (that implement java.util.Map)
* Java classes with bean getter/setters
* Scala classes with public vars
* Scala classes with java style getter/setter methods
* Scala case classes
* Classes initialized through a constructor only
* Classes with type parameters
* Allows choosing between different option treatments for formats that support omission instead of null