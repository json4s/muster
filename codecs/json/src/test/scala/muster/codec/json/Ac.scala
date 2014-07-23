package muster.codec.json

import muster.{Consumer, Junk}

class Ac {
  type Foo = Junk

  object WithAlias {
    implicit val WithAliasConsumer = Consumer.consumer[WithAlias]
  }

  case class WithAlias(in: Foo)

  case class NoAlias(in: Junk)

}