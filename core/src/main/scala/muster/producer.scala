package muster

import muster.jackson.util.ISO8601Utils

import scala.annotation.implicitNotFound
import scala.language.experimental.macros
import scala.language.higherKinds
import scala.reflect.macros._
import java.util.Date
import scala.reflect.ClassTag
import java.text.DateFormat
import scala.collection.concurrent.TrieMap


/** Receives a value and pushes that into the output formatter
  *
  * This is the main extension point for registering custom serializers
  *
  * @example a custom serializer for a person
  *          {{{
  *          case class Address(firstLine: String, secondLine: Option[String], postcode: String, state: String, country: String)
  *          case class Person(id: Int, name: String, addresses: Seq[Address])
  *
  *          implicit object PersonProducer extends Producer[Person] {
  *            def produce(value: Person, formatter: OutputFormatter[_]) {
  *              formatter.startObject()
  *
  *              formatter.startField("id")
  *              formatter.int(value.id)
  *
  *              formatter.startField("name")
  *              formatter.string(person.name)
  *
  *              val arrProducer = implicitly[Producer[Seq[Address]]]
  *              arrProducer.produce(value.addresses, formatter)
  *
  *              formatter.endObject()
  *            }
  *          }
  *          }}}
  *
  * @example a producer for a string
  *          {{{
  *            new Producer[String] {
  *              def produce(value: String, formatter: OutputFormatter[_]) {
  *                formatter.string(value)
  *              }
  *            }
  *          }}}
  *
  * @tparam T the type of value this producer knows about
  */
@implicitNotFound("Couldn't find a producer for ${T}. Try importing muster._ or to implement a muster.Consumable")
trait Producer[T] {
  def produce(value: T, formatter: OutputFormatter[_])
}

/** The companion object for a [[muster.Producer]]
  *
  * This object holds the macro that generates the ad-hoc producers and all the predefined conversions
  */
object Producer {
  private[Producer] abstract class SP[T](fn: (OutputFormatter[_], T) => Unit) extends Producer[T] {
    def produce(value: T, formatter: OutputFormatter[_]): Unit = fn(formatter, value)
  }
  private[Producer] def sp[T](fn: (OutputFormatter[_], T) => Unit): Producer[T] = new SP[T](fn) {}

  /** serializes byte values */
  implicit object ByteProducer extends SP[Byte](_ byte _)

  /** serializes short values */
  implicit object ShortProducer extends SP[Short](_ short _)

  /** serializes int values */
  implicit object IntProducer extends SP[Int](_ int _)

  /** serializes long values */
  implicit object LongProducer extends SP[Long](_ long _)

  /** serializes big int values */
  implicit object BigIntProducer extends SP[BigInt](_ bigInt _)

  /** serializes float values */
  implicit object FloatProducer extends SP[Float](_ float _)

  /** serializes double values */
  implicit object DoubleProducer extends SP[Double](_ double _)

  /** serializes big decimal values */
  implicit object BigDecimalProducer extends SP[BigDecimal](_ bigDecimal _)

  /** serializes byte values */
  implicit object JavaByteProducer extends SP[java.lang.Byte](_ byte _)

  /** serializes short values */
  implicit object JavaShortProducer extends SP[java.lang.Short](_ short _)

  /** serializes int values */
  implicit object JavaIntProducer extends SP[java.lang.Integer](_ int _)

  /** serializes long values */
  implicit object JavaLongProducer extends SP[java.lang.Long](_ long _)

  /** serializes big int values */
  implicit object JavaBigIntProducer extends SP[java.math.BigInteger](_ bigInt _)

  /** serializes float values */
  implicit object JavaFloatProducer extends SP[java.lang.Float](_ float _)

  /** serializes double values */
  implicit object JavaDoubleProducer extends SP[java.lang.Double](_ double _)

  /** serializes big decimal values */
  implicit object JavaBigDecimalProducer extends SP[java.math.BigDecimal](_ bigDecimal _)

  /** serializes boolean values */
  implicit object BooleanProducer extends SP[Boolean](_ boolean _)

  /** serializes boolean values */
  implicit object JavaBooleanProducer extends SP[java.lang.Boolean](_ boolean _)

  /** serializes string values */
  implicit object StringProducer extends SP[String](_ string _)

  /** serializes symbol values */
  implicit object SymbolProducer extends SP[scala.Symbol](_ string _.name)

  /** serializes map values */
  implicit def mapProducer[F[_, _] <: collection.Map[_, _], K, V](implicit keySerializer: MapKeySerializer[K], valueProducer: Producer[V]): Producer[F[K, V]] = {
    new Producer[F[K, V]] {
      def produce(value: F[K, V], formatter: OutputFormatter[_]) {
        formatter.startObject(value.getClass.getName)
        val v = value.asInstanceOf[collection.Map[K, V]]
        v.foreach { (kv: (K, V)) =>
          formatter.startField(keySerializer.serialize(kv._1))
          valueProducer.produce(kv._2, formatter)
        }
        formatter.endObject()
      }
    }
  }

  /** serializes map values */
  implicit def javaMapProducer[K, T](implicit keySerializer: MapKeySerializer[K], valueProducer: Producer[T]): Producer[java.util.Map[K, T]] =
    sp[java.util.Map[K, T]] { (fmt, v) =>
      fmt.startObject(v.getClass.getSimpleName)
      val iter = v.entrySet().iterator()
      while(iter.hasNext) {
        val kv = iter.next()
        fmt.startField(keySerializer.serialize(kv.getKey))
        valueProducer.produce(kv.getValue, fmt)
      }
      fmt.endObject()
    }

  /** serializes array values */
  implicit def arrayProducer[T](implicit ct: ClassTag[T], valueProducer: Producer[T]): Producer[Array[T]] =
    sp[Array[T]] { (fmt, arr) =>
      fmt.startArray("Array")
      arr.foreach(valueProducer.produce(_, fmt))
      fmt.endArray()
    }

  /** serializes traversable values */
  implicit def traversableProducer[C[_] <: Traversable[_], T](implicit valueProducer: Producer[T]): Producer[C[T]] =
    sp[C[T]] { (fmt, v) =>
      fmt.startArray(v.getClass.getSimpleName)
      v.asInstanceOf[Traversable[T]].foreach(vv => valueProducer.produce(vv, fmt))
      fmt.endArray()
    }

  /** serializes java list values */
  implicit def javaListProducer[T](implicit valueProducer: Producer[T]) =
    sp[java.util.List[T]] { (fmt, v) =>
      fmt.startArray(v.getClass.getSimpleName)
      val iter = v.iterator()
      while(iter.hasNext) {
        valueProducer.produce(iter.next(), fmt)
      }
      fmt.endArray()
    }

  /** serializes java set values */
  implicit def javaSetProducer[T](implicit valueProducer: Producer[T]) =
    sp[java.util.Set[T]] { (fmt, v) =>
      fmt.startArray(v.getClass.getSimpleName)
      val iter = v.iterator()
      while(iter.hasNext) { valueProducer.produce(iter.next(), fmt) }
      fmt.endArray()
    }

  /** serializes option values */
  implicit def optionProducer[T](implicit valueProducer: Producer[T]): Producer[Option[T]] =
    sp[Option[T]] { (fmt, v) =>
      if (v.isDefined) valueProducer.produce(v.get, fmt)
      else fmt.writeNull()
    }

  private[this] val safeFormatterPool = TrieMap.empty[String, DateFormat]

  /** Creates a date producer for the specified pattern
    * 
    * The date producer is backed by a java.text.DateFormat but a thread-safe one
    * @param pattern the [[java.text.SimpleDateFormat]] pattern to use
    * @return 
    */
  def dateProducer(pattern: String): Producer[Date] = {
    dateProducerFromFormat(safeFormatterPool.getOrElseUpdate(pattern, new util.SafeSimpleDateFormat(pattern)))
  }

  /** Creates a date producer from a date format, make sure the date format is thread safe
    *
    * @param format the [[java.text.DateFormat]] to use for this date producer
    * @return the [[muster.Producer]] for a [[java.util.Date]]
    */
  def dateProducerFromFormat(format: DateFormat): Producer[Date] = sp[Date]((fmt, v) => fmt string format.format(v))

  /** Default ISO8601 date format producer for the pattern yyyy-MM-ddThh:mm:ss[.sss][Z|[+-]hh:mm] */
  implicit val Iso8601DateProducer: Producer[Date] = sp[Date]((fmt, v) => fmt.string(ISO8601Utils.format(v)))

  implicit def producer[T]: Producer[T] = macro producerImpl[T]

  def producerImpl[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[Producer[T]] = {
    import c.universe._
    val helper = new Helper[c.type](c)
    val tpe = weakTypeOf[T].dealias

    def buildObject(target: Tree, formatter: Tree): Tree = {
      val TypeRef(_, sym: Symbol, _) = tpe
      val fields = helper.getGetters(tpe)
       val fieldTrees = fields map { fld =>
        val tt = fld.asMethod.typeSignatureIn(tpe).resultType
        val on = fld.name.decodedName.toString.trim
        val needsLower = on.startsWith("get")
        val stripped = on.replaceFirst("^get", "")
        val fieldName = if (needsLower) stripped(0).toLower + stripped.substring(1) else stripped
        val fieldPath = Select(target, fld.asTerm.name)
        // TODO: Add field renaming strategy
        val startFieldExpr = Apply(Select(formatter, TermName("startField")), Literal(Constant(fieldName)) :: Nil)

        val pTpe = appliedType(weakTypeOf[Producer[Any]], tt::Nil)
        val fVal: List[Tree] = c.inferImplicitValue(pTpe) match {
          case EmptyTree =>
            c.error(c.enclosingPosition, s"Couldn't find an implicit $pTpe, try defining one or bringing one into scope")
            // error returns unit
            c.abort(c.enclosingPosition, s"Couldn't find an implicit $pTpe, try defining one or bringing one into scope")
          case x =>
            val pn = c.freshName("producer$")
            val ptn = TermName(pn)
            val pv: Tree = ValDef(Modifiers(), ptn, TypeTree(pTpe), x)
            val fn = c.freshName("value$")
            val ftn = TermName(fn)
            val fv: Tree = ValDef(Modifiers(), ftn, TypeTree(tt), fieldPath)
            val write: Tree = Apply(Select(Ident(ptn), TermName("produce")), Ident(ftn) :: formatter :: Nil)
            fv :: pv ::  write :: Nil
        }
        startFieldExpr :: fVal
      }
      Block(
        Apply(
          Select(formatter, TermName("startObject")), 
          Literal(Constant(sym.name.decodedName.toString))::Nil) :: 
        fieldTrees.reverse.flatten,
        Apply(Select(formatter, TermName("endObject")), Nil))

    }


    if (tpe.typeSymbol.isClass && !(helper.isPrimitive(tpe) || helper.isMap(tpe) || helper.isOption(tpe) || helper.isSeq(tpe))) {
      reify {
        new Producer[T] {
          def produce(value: T, formatter: OutputFormatter[_]): Unit = {
            c.Expr(buildObject(Ident(TermName("value")), Ident(TermName("formatter")))).splice
          }
        }
      }
    } else {
      c.abort(c.enclosingPosition, "Values, Lists, Options and Maps don't use macros")
    }
  }
}
