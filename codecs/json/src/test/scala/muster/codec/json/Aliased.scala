package muster.codec.json

import muster.Junk

//import org.joda.time.DateTime
object Aliased {
  type Foo = Junk

  //  object WithAlias {
  //    implicit val WithAliasConsumer = Consumer.consumer[WithAlias]
  //  }

  case class WithAlias(in: Foo)

}