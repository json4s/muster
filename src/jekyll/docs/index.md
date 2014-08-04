---
layout: default
---

# Muster

Muster is a simple library that allows you to compose and decompose objects based on map like formats like json.  It supports streaming reading and writing so your structures don't need to have 2 complete representations in memory. 

It requires no extra code for serializing and uses macros to generate serializers and deserializers at compile time. The goal of muster is to avoid runtime reflection if it can be avoided. So far we don't need reflection and everything is resolved at compile time.

## Documentation

* [Supported features](features.html)
* [Custom serializers](custom_serializers.html)
* [Using case classes as string](string_codec.html)
* [Json codecs](json_codecs.html)
  * [Jawn Codec](jawn_codec.html)
  * [Jackson Codec](jackson_codec.html)
  * [Json4s Codec](json4s_codec.html)
  * [Play Json Codec](play_json_codec.html)
  * [Argonaut Codec](argonaut_codec.html)
* [Extending muster](extending.html)
  * [Integrate a parser example](integrate_parser.html)
  * [Provide a custom output format](custom_renderer.html)
  * [Integrate an AST](integrate_ast.html)
