package muster


package object aliasing {
  type Foo = Junk

  object WithAlias {
    implicit val WithAliasConsumer = Consumer.consumer[_root_.muster.aliasing.WithAlias]
  }
  case class WithAlias(in: Foo)
}