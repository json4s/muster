package object muster {
//  import scala.language.implicitConversions

  implicit class ProducableProduct[T <: Product](p: T)(implicit prod: Producer[T])  {
    def asString: String = Muster.produce.String.from(p)
    def asJson = Muster.produce.Json.from(p)
  }

}
