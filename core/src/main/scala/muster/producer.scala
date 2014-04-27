package muster

import scala.language.experimental.macros
import scala.reflect.macros._
import java.util.Date
import scala.reflect.ClassTag
import java.text.DateFormat
import scala.collection.concurrent.TrieMap

object Producer {
  private[Producer] abstract class SP[T](fn: (OutputFormatter[_], T) => Unit) extends Producer[T] {
    def produce(value: T, formatter: OutputFormatter[_]): Unit = fn(formatter, value)
  }
  private def sp[T](fn: (OutputFormatter[_], T) => Unit): Producer[T] = new Producer[T] {
    def produce(value: T, formatter: OutputFormatter[_]): Unit = fn(formatter, value)
  }
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

  implicit def mapProducer[T](implicit valueProducer: Producer[T]): Producer[collection.GenMap[String, T]] =
    sp[collection.GenMap[String, T]] { (fmt, v) =>
      fmt.startObject(v.getClass.getSimpleName)
      v foreach { kv =>
        fmt.startField(kv._1)
        valueProducer.produce(kv._2, fmt)
      }
      fmt.endObject()
    }

  implicit def seqProducer[T](implicit valueProducer: Producer[T]): Producer[Seq[T]] =
    sp[Seq[T]] { (fmt, v) =>
      fmt.startArray(v.getClass.getSimpleName)
      v foreach (valueProducer.produce(_, fmt))
      fmt.endArray()
    }

  implicit def setProducer[T](implicit valueProducer: Producer[T]): Producer[Set[T]] =
    sp[Set[T]] { (fmt, v) =>
      fmt.startArray(v.getClass.getSimpleName)
      v foreach (valueProducer.produce(_, fmt))
      fmt.endArray()
    }


  implicit def optionProducer[T](implicit valueProducer: Producer[T]): Producer[Option[T]] =
    sp[Option[T]] { (fmt, v) => v foreach (valueProducer.produce(_, fmt))}

  private[this] val safeFormatterPool = TrieMap.empty[String, DateFormat]
  def dateProducer(pattern: String) = {
    dateProducerFromFormat(safeFormatterPool.getOrElseUpdate(pattern, new SafeSimpleDateFormat(pattern)))
  }
  def dateProducerFromFormat(format: DateFormat) = sp[Date]((fmt, v) => fmt string format.format(v))

  implicit val DefaultDateProducer = dateProducerFromFormat(SafeSimpleDateFormat.Iso8601Formatter)

  implicit def producer[T]: Producer[T] = macro producerImpl[T]

  def producerImpl[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[Producer[T]] = {
    import c.universe._
    val helper = new Helper[c.type](c)
    val tpe = weakTypeOf[T].dealias

    def buildObject(target: Tree, formatter: Tree): Tree = {
      val TypeRef(_, sym: Symbol, _) = tpe
      val fields = helper.getGetters(tpe)
       val fieldTrees = fields map { fld =>
        val tt = fld.asMethod.returnType
        val on = fld.name.decodedName.toString.trim
        val needsLower = on.startsWith("get")
        val stripped = on.replaceFirst("^get", "")
        val fieldName = if (needsLower) stripped(0).toLower + stripped.substring(1) else stripped
        val fieldPath = Select(target, fld.asTerm.name)
        val startFieldExpr = Apply(Select(formatter, TermName("startField")), Literal(Constant(fieldName)) :: Nil)
        val pTpe = appliedType(weakTypeOf[Producer[Any]], tt::Nil)
        val fVal: List[Tree] = c.inferImplicitValue(pTpe) match {
          case EmptyTree =>
            c.error(c.enclosingPosition, s"Couldn't find an implicit ${pTpe}, try defining one or bringing one into scope")
            // error returns unit
            c.abort(c.enclosingPosition, s"Couldn't find an implicit ${pTpe}, try defining one or bringing one into scope")
//            Nil
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
        Apply(Select(formatter, TermName("startObject")), Literal(Constant(sym.name.decodedName.toString))::Nil) ::
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
    } else c.abort(c.enclosingPosition, "Values, Lists, Options and Maps don't use implicits")
  }
}

trait Producer[T] {
  def produce(value: T, formatter: OutputFormatter[_])
}

//trait Producer[T] {
//  def writeFormatted[R](value: T, outputFormat: OutputFormat[R]): R
//}
//
//object Producer {
//
//
//  implicit def producer[T]: Producer[T] = macro producerImpl[T]
//
//  def producerImpl[T: c.WeakTypeTag](c: Context): c.Expr[Producer[T]] = {
//    import c.universe._
//    import definitions._
//    import Flag._
//    val helper = new Helper[c.type](c)
//
//    val sw = c.Expr[java.io.StringWriter](Ident(TermName("sw")))
//    val formatterStack = c.Expr[OutputFormatter[_]](Ident(TermName("formatter")))
//
//
//
//    val primitiveTypes =
//      Vector(
//        (typeOf[Int], (t: Tree) => reify {
//          formatterStack.splice.int(c.Expr[Int](t).splice)
//        }),
//        (typeOf[String], (t: Tree) => reify {
//          formatterStack.splice.string(c.Expr[String](t).splice)
//        }),
//        (typeOf[Float], (t: Tree) => reify {
//          formatterStack.splice.float(c.Expr[Float](t).splice)
//        }),
//        (typeOf[Double], (t: Tree) => reify {
//          formatterStack.splice.double(c.Expr[Double](t).splice)
//        }),
//        (typeOf[Boolean], (t: Tree) => reify {
//          formatterStack.splice.boolean(c.Expr[Boolean](t).splice)
//        }),
//        (typeOf[Long], (t: Tree) => reify {
//          formatterStack.splice.long(c.Expr[Long](t).splice)
//        }),
//        (typeOf[Byte], (t: Tree) => reify {
//          formatterStack.splice.byte(c.Expr[Byte](t).splice)
//        }),
//        (typeOf[BigInt], (t: Tree) => reify {
//          formatterStack.splice.bigInt(c.Expr[BigInt](t).splice)
//        }),
//        (typeOf[Short], (t: Tree) => reify {
//          formatterStack.splice.short(c.Expr[Short](t).splice)
//        }),
//        (typeOf[BigDecimal], (t: Tree) => reify {
//          formatterStack.splice.bigDecimal(c.Expr[BigDecimal](t).splice)
//        }),
//        (typeOf[java.lang.Integer], (t: Tree) => reify {
//          formatterStack.splice.int(c.Expr[Int](t).splice)
//        }),
//        (typeOf[java.lang.String], (t: Tree) => reify {
//          formatterStack.splice.string(c.Expr[String](t).splice)
//        }),
//        (typeOf[java.lang.Float], (t: Tree) => reify {
//          formatterStack.splice.float(c.Expr[Float](t).splice)
//        }),
//        (typeOf[java.lang.Double], (t: Tree) => reify {
//          formatterStack.splice.double(c.Expr[Double](t).splice)
//        }),
//        (typeOf[java.lang.Boolean], (t: Tree) => reify {
//          formatterStack.splice.boolean(c.Expr[Boolean](t).splice)
//        }),
//        (typeOf[java.lang.Long], (t: Tree) => reify {
//          formatterStack.splice.long(c.Expr[Long](t).splice)
//        }),
//        (typeOf[java.lang.Byte], (t: Tree) => reify {
//          formatterStack.splice.byte(c.Expr[Byte](t).splice)
//        }),
//        (typeOf[java.math.BigInteger], (t: Tree) => reify {
//          formatterStack.splice.bigInt(c.Expr[BigInt](t).splice)
//        }),
//        (typeOf[java.lang.Short], (t: Tree) => reify {
//          formatterStack.splice.short(c.Expr[Short](t).splice)
//        }),
//        (typeOf[java.math.BigDecimal], (t: Tree) => reify {
//          formatterStack.splice.bigDecimal(c.Expr[BigDecimal](t).splice)
//        }),
//        (typeOf[Date], (t: Tree) => reify {
//          formatterStack.splice.date(c.Expr[Date](t).splice)
//        }),
//        (typeOf[scala.Symbol], (t: Tree) => reify {
//          formatterStack.splice.string(c.Expr[scala.Symbol](t).splice.name)
//        }))
//
//    val collTpe = typeOf[scala.collection.GenSeq[_]]
//    val mapTpe = typeOf[scala.collection.GenMap[_, _]]
//
//    def writeList(tp: Type, target: Tree): c.Expr[Unit] = {
//      val TypeRef(_, _: Symbol, pTpe :: Nil) = tp
//       reify {
//        formatterStack.splice.startArray(c.Expr[String](Literal(Constant(tp.typeSymbol.name.decoded))).splice)
//        val coll = c.Expr[Seq[Any]](target).splice
//        coll foreach { ele =>
//          c.Expr(buildTpe(pTpe, Ident(TermName("ele")))).splice
//        }
//        formatterStack.splice.endArray()
//      }
//    }
//
//    def writeMap(tp: Type, target: Tree): c.Expr[Unit] = {
//      val TypeRef(_, _, keyTpe :: valTpe :: Nil) = tp
//      reify {
//        sw.splice.write(c.Expr[String](Literal(Constant(tp.typeSymbol.name.decoded))).splice)
//        sw.splice.write('(')
//        formatterStack.splice.startObject(tp.typeSymbol.name.decoded)
//        c.Expr[scala.collection.GenMap[_, _]](target).splice.foreach { case (k, v) =>
//          c.Expr(buildTpe(keyTpe, Ident(TermName("k")))).splice
//          sw.splice.write(' ')
//          sw.splice.write('-')
//          sw.splice.write('>')
//          sw.splice.write(' ')
//          c.Expr(buildTpe(valTpe, Ident(TermName("v")))).splice
//        }
//        formatterStack.splice.endObject()
//      }
//    }
//
//    def complexObject(tp: Type, target: Tree): c.Tree = {
//      val TypeRef(_, sym: Symbol, tpeArgs: List[Type]) = tp.normalize
////      val fields = helper.getGetters(tp)
//      val fields = helper.vals(tp) ++ helper.vars(tp)
//
//      val fieldTrees = fields flatMap { pSym =>
//        val tt = pSym.typeSignatureIn(tp)
//        val fieldName = pSym.name.decoded.trim
////        val on = pSym.name.decoded.trim
////        val needsLower = on.startsWith("get")
////        val stripped = on.replaceFirst("^get", "")
////        val fieldName = if (needsLower) stripped(0).toLower + stripped.substring(1) else stripped
//        val fieldPath = Select(target, TermName(fieldName))
//        val startFieldExpr =
//          Apply(Select(Ident(TermName("formatter")), TermName("startField")), Literal(Constant(fieldName)) :: Nil)
////        val startFieldExpr = reify {
////          formatterStack.splice.startField(c.Expr[String](Literal(Constant(fieldName))).splice)
////        }
//        val fval = buildTpe(tt, fieldPath)
//        fval :: startFieldExpr :: Nil
//      }
//      Block(
//        reify(formatterStack.splice.startObject(c.Expr[String](Literal(Constant(tp.typeSymbol.name.decoded))).splice)).tree ::
//          fieldTrees.toList.reverse :::
//          reify(formatterStack.splice.endObject()).tree ::
//          Nil, EmptyTree)
//    }
//
//    def buildTpe(tp: Type, target: Tree): Tree = {
//      primitiveTypes.find(_._1 =:= tp).map(_._2(target).tree) orElse {
//        if (tp <:< collTpe) Some(writeList(tp, target).tree) else None
//      } orElse {
//        if (tp <:< mapTpe) Some(writeMap(tp, target).tree) else None
//      } getOrElse {
//        complexObject(tp, target)
//      }
//    }
//
//    val tpe = weakTypeOf[T].normalize
//
//    val Block(formatableClass :: Nil, _) = {
//      reify {
//        final class $anon extends Producer[T] {
//          def writeFormatted[R](value: T, outputFormat: OutputFormat[R]): R = {
//            val sw = new java.io.StringWriter()
//            try {
//              val formatter = outputFormat.createFormatter
//              c.Expr(buildTpe(tpe, Ident(TermName("value")))).splice
//              formatter.result
//            } finally {
//              sw.close()
//            }
//          }
//        }
//      }.tree
//    }
//
//    c.Expr[Producer[T]](
//      Block(
//        List(formatableClass),
//        Apply(Select(New(Ident(newTypeName("$anon"))), nme.CONSTRUCTOR), List())
//      )
//    )
//  }
//}