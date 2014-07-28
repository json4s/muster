---
layout: default
---
# Extending muster

The overall design of muster is quite simple, and focusses on getting the job done properly. It is a library that works at compile time, so it generates type classes ad-hoc if the compiler can't resolve one first. This type class mechanism is the way to provide custom functionality like custom serialization or deserialization of a particular type. When the type class can't be resolved it will let you know with a friendly compiler error message. 

Muster is designed as an AST adapter. So it has its own bridging AST that serves as a bridge between various parsers, writers and AST's. This allows muster to support BSON, JSON, strings, custom byte based codecs like length delimited. In some cases this bridging AST is virtualized, because it only materializes one or a few nodes at a time instead of the entire stream. 

### Serialization

Muster provides an interface that is pretty much a [builder](https://github.com/json4s/muster/blob/master/core/src/main/scala/muster/output.scala#L6-L44). Streaming writers can be fit into this as being builders that build `Unit`.  It's not a clean cut builder because it has a close method, this allows for a java.io.Writer to be closed when the job is done. 

Those builders are called output formatters and are used by producers to write values onto a target like a file output stream. So the goal of those builders it to produce a value which is built piecemeal by a [producer](https://github.com/json4s/muster/blob/master/core/src/main/scala/muster/producer.scala#L226-L228).  
That value is defined by the builder. A producers produces values into a [producible](https://github.com/json4s/muster/blob/master/core/src/main/scala/muster/producible.scala#L22-L38). 

This allows for writing to streams, byte arrays, strings and anything you can define a builder for. Part of an AST integration is providing a builder for that ast type for example.

### Deserialization

Muster provides an interface that is pretty much a [reader](https://github.com/json4s/muster/blob/master/core/src/main/scala/muster/input.scala#L509-L573), and in muster that is called an `AstCursor`.  These cursors read values and turn them into the bridging AST nodes.
So a cursor provides an abstraction over the parsers of various formats. An AstCursor gets its values from a [consumable](https://github.com/json4s/muster/blob/master/core/src/main/scala/muster/consumable.scala#L5-L10) and gives them to a [consumer](https://github.com/json4s/muster/blob/master/core/src/main/scala/muster/consumer.scala#L14-L16) who creates an object instance of the requested type.

### Examples

* [Integrating a parser](integrate_parser.html)
* [Writing your renderer](custom_renderer.html)
* [Integrating an AST](integrate_ast.html)

