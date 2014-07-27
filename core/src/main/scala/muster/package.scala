package object muster {

  type Consumable[T] = input.Consumable[T]
  val Consumable = input.Consumable
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

  type Consumer[T] = input.Consumer[T]
  val Consumer = input.Consumer

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
  
  
  type Producer[T] = output.Producer[T]
  val Producer = output.Producer
  

}
