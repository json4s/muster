package muster

import scala.annotation.implicitNotFound
import scala.language.experimental.macros
import scala.reflect.macros._
import java.util.Date
import scala.reflect.ClassTag
import java.text.DateFormat
import scala.collection.concurrent.TrieMap
import scala.collection.immutable
import scala.collection.mutable


@implicitNotFound("Couldn't find a producer for ${T}. Try importing muster._ or to implement a muster.Producer")
trait Producer[T] {
  def produce(value: T, formatter: OutputFormatter[_])
}

object Producer {
  private[Producer] abstract class SP[T](fn: (OutputFormatter[_], T) => Unit) extends Producer[T] {
    def produce(value: T, formatter: OutputFormatter[_]): Unit = fn(formatter, value)
  }
  private[Producer] def sp[T](fn: (OutputFormatter[_], T) => Unit): Producer[T] = new SP[T](fn) {}
  implicit object ByteProducer extends SP[Byte](_ byte _)
  implicit object ShortProducer extends SP[Short](_ short _)
  implicit object IntProducer extends SP[Int](_ int _)
  implicit object LongProducer extends SP[Long](_ long _)
  implicit object BigIntProducer extends SP[BigInt](_ bigInt _)
  implicit object FloatProducer extends SP[Float](_ float _)
  implicit object DoubleProducer extends SP[Double](_ double _)
  implicit object BigDecimalProducer extends SP[BigDecimal](_ bigDecimal _)
  implicit object JavaByteProducer extends SP[java.lang.Byte](_ byte _)
  implicit object JavaShortProducer extends SP[java.lang.Short](_ short _)
  implicit object JavaIntProducer extends SP[java.lang.Integer](_ int _)
  implicit object JavaLongProducer extends SP[java.lang.Long](_ long _)
  implicit object JavaBigIntProducer extends SP[java.math.BigInteger](_ bigInt _)
  implicit object JavaFloatProducer extends SP[java.lang.Float](_ float _)
  implicit object JavaDoubleProducer extends SP[java.lang.Double](_ double _)
  implicit object JavaBigDecimalProducer extends SP[java.math.BigDecimal](_ bigDecimal _)
  implicit object BooleanProducer extends SP[Boolean](_ boolean _)
  implicit object JavaBooleanProducer extends SP[java.lang.Boolean](_ boolean _)
  implicit object StringProducer extends SP[String](_ string _)
  implicit object SymbolProducer extends SP[scala.Symbol](_ string _.name)

  implicit def arrayProducer[T](implicit ct: ClassTag[T], valueProducer: Producer[T]): Producer[Array[T]] =
    sp[Array[T]] { (fmt, arr) =>
      fmt.startArray("Array")
      arr.foreach(valueProducer.produce(_, fmt))
      fmt.endArray()
    }

  implicit def genMapProducer[K, T](implicit keySerializer: MapKeySerializer[K], valueProducer: Producer[T]): Producer[collection.GenMap[K, T]] =
    sp[collection.GenMap[K, T]] { (fmt, v) =>
      fmt.startObject(v.getClass.getSimpleName)
      v foreach { kv =>
        fmt.startField(keySerializer.serialize(kv._1))
        valueProducer.produce(kv._2, fmt)
      }
      fmt.endObject()
    }

  implicit def mapProducer[K, T](implicit keySerializer: MapKeySerializer[K], valueProducer: Producer[T]): Producer[immutable.Map[K, T]] =
    sp[immutable.Map[K, T]] { (fmt, v) =>
      fmt.startObject(v.getClass.getSimpleName)
      v foreach { kv =>
        fmt.startField(keySerializer.serialize(kv._1))
        valueProducer.produce(kv._2, fmt)
      }
      fmt.endObject()
    }

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


//  implicit class traversableProducer[C, T <: Traversable[C]]
  implicit def traversableProducer[C](implicit valueProducer: Producer[C]) =
    sp[collection.Traversable[C]] { (fmt, v) =>
      fmt.startArray(v.getClass.getSimpleName)
      v foreach (valueProducer.produce(_, fmt))
      fmt.endArray()
    }

  implicit def listProducer[T](implicit valueProducer: Producer[T]) =
    sp[immutable.List[T]] { (fmt, v) =>
      fmt.startArray(v.getClass.getSimpleName)
      v foreach (valueProducer.produce(_, fmt))
      fmt.endArray()
    }
  implicit def javaListProducer[T](implicit valueProducer: Producer[T]) =
    sp[java.util.List[T]] { (fmt, v) =>
      fmt.startArray(v.getClass.getSimpleName)
      val iter = v.iterator()
      while(iter.hasNext) {
        valueProducer.produce(iter.next(), fmt)
      }
      fmt.endArray()
    }

  implicit def vectorProducer[T](implicit valueProducer: Producer[T]) =
    sp[immutable.Vector[T]] { (fmt, v) =>
      fmt.startArray(v.getClass.getSimpleName)
      v foreach (valueProducer.produce(_, fmt))
      fmt.endArray()
    }

  implicit def mutableListProducer[T](implicit valueProducer: Producer[T]) =
    sp[mutable.ListBuffer[T]] { (fmt, v) =>
      fmt.startArray(v.getClass.getSimpleName)
      v foreach (valueProducer.produce(_, fmt))
      fmt.endArray()
    }
  implicit def seqProducer[T](implicit valueProducer: Producer[T]) =
    sp[Seq[T]] { (fmt, v) =>
      fmt.startArray(v.getClass.getSimpleName)
      v foreach (valueProducer.produce(_, fmt))
      fmt.endArray()
    }
  implicit def setProducer[T](implicit valueProducer: Producer[T]) =
    sp[immutable.Set[T]] { (fmt, v) =>
      fmt.startArray(v.getClass.getSimpleName)
      v foreach (valueProducer.produce(_, fmt))
      fmt.endArray()
    }
  implicit def javaSetProducer[T](implicit valueProducer: Producer[T]) =
    sp[java.util.Set[T]] { (fmt, v) =>
      fmt.startArray(v.getClass.getSimpleName)
      val iter = v.iterator()
      while(iter.hasNext) { valueProducer.produce(iter.next(), fmt) }
      fmt.endArray()
    }
  implicit def mutableSeqProducer[T](implicit valueProducer: Producer[T]) =
    sp[mutable.Seq[T]] { (fmt, v) =>
      fmt.startArray(v.getClass.getSimpleName)
      v foreach (valueProducer.produce(_, fmt))
      fmt.endArray()
    }
  implicit def mutableSetProducer[T](implicit valueProducer: Producer[T]) =
    sp[mutable.Set[T]] { (fmt, v) =>
      fmt.startArray(v.getClass.getSimpleName)
      v foreach (valueProducer.produce(_, fmt))
      fmt.endArray()
    }

  implicit def optionProducer[T](implicit valueProducer: Producer[T]): Producer[Option[T]] =
    sp[Option[T]] { (fmt, v) =>
      if (v.isDefined) valueProducer.produce(v.get, fmt)
      else fmt.writeNull()
    }

  private[this] val safeFormatterPool = TrieMap.empty[String, DateFormat]
  def dateProducer(pattern: String) = {
    dateFromFormat(safeFormatterPool.getOrElseUpdate(pattern, new SafeSimpleDateFormat(pattern)))
  }
  def dateFromFormat(format: DateFormat) = sp[Date]((fmt, v) => fmt string format.format(v))

  implicit val DefaultDateProducer = dateFromFormat(SafeSimpleDateFormat.Iso8601Formatter)

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
            c.error(c.enclosingPosition, s"Couldn't find an implicit ${pTpe}, try defining one or bringing one into scope")
            // error returns unit
            c.abort(c.enclosingPosition, s"Couldn't find an implicit ${pTpe}, try defining one or bringing one into scope")
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
