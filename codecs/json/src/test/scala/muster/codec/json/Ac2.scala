package muster.codec.json

import muster.Junk

class Ac2 {
  type Foo = Junk

  case class WithAlias(in: Foo)

  case class NoAlias(in: Junk)

}