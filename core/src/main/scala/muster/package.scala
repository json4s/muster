/** Muster is a library that leverages macros to generate serializers for your objects
  *
  * A library for macro based serializers to different formats.
  * It uses scala macros so no reflection is involved and it will generate code at compile time
  * that kind of looks like it would have been handwritten.  It is written with the idea of extension, so it's easy to
  * add your own formats.
  *
  * You can find [[https://muster.json4s.org/docs/ more documentation on the wiki]]
  *
  * Getting the library
  *
  * This works with scala 2.10 and up.
  * Check the [[https://github.com/json4s/muster/releases releases page]] for the latest version.
  * The library is published to maven central so you can get it with:
  *
  * {{{
  * libraryDependencies += "org.json4s" %% "muster-codec-jawn" % "latest"
  * libraryDependencies += "org.json4s" %% "muster-codec-jackson" % "latest"
  * }}}
  *
  * @example Bring your own AST (BYA):
  *
  * {{{
  * libraryDependencies += "org.json4s" %% "muster-codec-json4s" % "latest"
  * }}}

  * @example Prettier case class for debugging:
  *
  * {{{
  * libraryDependencies += "org.json4s" %% "muster-codec-string" % "latest"
  * }}}
  *
  */
package object muster {

  type Consumable[T] = input.Consumable[T]
  val Consumable = input.Consumable

  type Consumer[T] = input.Consumer[T]
  val Consumer = input.Consumer

  type Producer[T] = output.Producer[T]
  val Producer = output.Producer

  type ByteArrayConsumable = input.ByteArrayConsumable
  val ByteArrayConsumable = input.ByteArrayConsumable
  type ByteBufferConsumable = input.ByteBufferConsumable
  val ByteBufferConsumable = input.ByteBufferConsumable
  type ByteChannelConsumable = input.ByteChannelConsumable
  val ByteChannelConsumable = input.ByteChannelConsumable
  type FileConsumable = input.FileConsumable
  val FileConsumable = input.FileConsumable
  type InputStreamConsumable = input.InputStreamConsumable
  val InputStreamConsumable = input.InputStreamConsumable
  type ReaderConsumable = input.ReaderConsumable
  val ReaderConsumable = input.ReaderConsumable
  type StringConsumable = input.StringConsumable
  val StringConsumable = input.StringConsumable
  type URLConsumable = input.URLConsumable
  val URLConsumable = input.URLConsumable

  type Producible[T, R] = output.Producible[T, R]
  val Producible = output.Producible
  type FileProducible = output.FileProducible
  val FileProducible = output.FileProducible
  type OutputStreamProducible = output.OutputStreamProducible
  val OutputStreamProducible = output.OutputStreamProducible
  type ByteChannelProducible = output.ByteChannelProducible
  val ByteChannelProducible = output.ByteChannelProducible
  type WriterProducible = output.WriterProducible
  val WriterProducible = output.WriterProducible
  val ByteArrayProducible = output.ByteArrayProducible
  val ByteBufferProducible = output.ByteBufferProducible
  val StringProducible = output.StringProducible
  
  

}
