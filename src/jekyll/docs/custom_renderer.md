---
layout: default
---
# Custom Renderer

A custom output format is used to turn a rendered AST into a stream or serialized format like a string or an array of bytes.

This example will explain the roles of the classes involved in rendering a muster AST to a stream, string,...

To provide a custom renderer you need to provide a formatter first. Below is the code for the JValueFormatter

{% code_ref ../../codecs/strings/src/main/scala/muster/codec/string/string_output.scala string_output_formatter %}

This class uses stacks to keep track of nested objects and a stack of states to indicate what it it is currently building. The most important method in this class is the writeValue method which produces the result value at the end of the effort.
The formatter class is the one that does all the work. A renderer uses a formatter to write to a consumable.

The renderer itself is quite simple

{% code_ref ../../codecs/strings/src/main/scala/muster/codec/string/string_output.scala string_renderer %}

