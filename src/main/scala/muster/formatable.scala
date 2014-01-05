package muster

import scala.language.experimental.macros
import scala.reflect.macros._
import java.util.Date
import com.fasterxml.jackson.databind.JsonNode
import scala.util.Try
import com.fasterxml.jackson.databind.node.MissingNode

object Muster {
  object produce {

    object String extends DefaultStringFormat

    object CompactJsonString extends JsonOutput

  }

  object consume {

    object Json extends JacksonInputFormat[Consumable[_]] {

      private def jic[T](src: T)(fn: (T) => JsonNode): JacksonInputCursor[T] = new JacksonInputCursor[T] {
        protected val node: JsonNode = Try(fn(src)).getOrElse(MissingNode.getInstance())
        val source: T = src
      }

      def createCursor(in: Consumable[_]): JacksonInputCursor[_] = in match {
        case StringConsumable(src) => jic(src)(mapper.readTree)
        case FileConsumable(src) => jic(src)(mapper.readTree)
        case ReaderConsumable(src) => jic(src)(mapper.readTree)
        case InputStreamConsumable(src) => jic(src)(mapper.readTree)
        case ByteArrayConsumable(src) => jic(src)(mapper.readTree)
        case URLConsumable(src) => jic(src)(mapper.readTree)
      }
    }

  }

}

trait Producer[T] {
  def writeFormatted[R](value: T, outputFormat: OutputFormat[R]): R
}

object Producer {

  import scala.language.implicitConversions
  class ProducableProduct[T <: Product](p: T)(implicit prod: Producer[T])
  implicit def producableProduct[T <: Product](p: T)(implicit prod: Producer[T]) = new ProducableProduct[T](p){
    override def toString: String = Muster.produce.String.from(p)
    def toJson = Muster.produce.CompactJsonString.from(p)
  }


  implicit def formatable[T]: Producer[T] = macro formatableImpl[T]

  def formatableImpl[T: c.WeakTypeTag](c: Context): c.Expr[Producer[T]] = {
    import c.universe._
    import definitions._
    import Flag._
    val helper = new Helper[c.type](c)

    val sw = c.Expr[java.io.StringWriter](Ident(newTermName("sw")))
    val formatterStack = c.Expr[OutputFormatter[_]](Ident(newTermName("formatter")))

    val primitiveTypes =
      Vector(
        (typeOf[Int], (t: Tree) => reify {
          formatterStack.splice.int(c.Expr[Int](t).splice)
        }),
        (typeOf[String], (t: Tree) => reify {
          formatterStack.splice.string(c.Expr[String](t).splice)
        }),
        (typeOf[Float], (t: Tree) => reify {
          formatterStack.splice.float(c.Expr[Float](t).splice)
        }),
        (typeOf[Double], (t: Tree) => reify {
          formatterStack.splice.double(c.Expr[Double](t).splice)
        }),
        (typeOf[Boolean], (t: Tree) => reify {
          formatterStack.splice.boolean(c.Expr[Boolean](t).splice)
        }),
        (typeOf[Long], (t: Tree) => reify {
          formatterStack.splice.long(c.Expr[Long](t).splice)
        }),
        (typeOf[Byte], (t: Tree) => reify {
          formatterStack.splice.byte(c.Expr[Byte](t).splice)
        }),
        (typeOf[BigInt], (t: Tree) => reify {
          formatterStack.splice.bigInt(c.Expr[BigInt](t).splice)
        }),
        (typeOf[Short], (t: Tree) => reify {
          formatterStack.splice.short(c.Expr[Short](t).splice)
        }),
        (typeOf[BigDecimal], (t: Tree) => reify {
          formatterStack.splice.bigDecimal(c.Expr[BigDecimal](t).splice)
        }),
        (typeOf[java.lang.Integer], (t: Tree) => reify {
          formatterStack.splice.int(c.Expr[Int](t).splice)
        }),
        (typeOf[java.lang.String], (t: Tree) => reify {
          formatterStack.splice.string(c.Expr[String](t).splice)
        }),
        (typeOf[java.lang.Float], (t: Tree) => reify {
          formatterStack.splice.float(c.Expr[Float](t).splice)
        }),
        (typeOf[java.lang.Double], (t: Tree) => reify {
          formatterStack.splice.double(c.Expr[Double](t).splice)
        }),
        (typeOf[java.lang.Boolean], (t: Tree) => reify {
          formatterStack.splice.boolean(c.Expr[Boolean](t).splice)
        }),
        (typeOf[java.lang.Long], (t: Tree) => reify {
          formatterStack.splice.long(c.Expr[Long](t).splice)
        }),
        (typeOf[java.lang.Byte], (t: Tree) => reify {
          formatterStack.splice.byte(c.Expr[Byte](t).splice)
        }),
        (typeOf[java.math.BigInteger], (t: Tree) => reify {
          formatterStack.splice.bigInt(c.Expr[BigInt](t).splice)
        }),
        (typeOf[java.lang.Short], (t: Tree) => reify {
          formatterStack.splice.short(c.Expr[Short](t).splice)
        }),
        (typeOf[java.math.BigDecimal], (t: Tree) => reify {
          formatterStack.splice.bigDecimal(c.Expr[BigDecimal](t).splice)
        }),
        (typeOf[Date], (t: Tree) => reify {
          formatterStack.splice.date(c.Expr[Date](t).splice)
        }),
        (typeOf[scala.Symbol], (t: Tree) => reify {
          formatterStack.splice.string(c.Expr[scala.Symbol](t).splice.name)
        }))

    val collTpe = typeOf[scala.collection.GenSeq[_]]
    val mapTpe = typeOf[scala.collection.GenMap[_, _]]

    def writeList(tp: Type, target: Tree): c.Expr[Unit] = {
      val TypeRef(_, _: Symbol, pTpe :: Nil) = tp
      reify {
        formatterStack.splice.startArray(c.Expr[String](Literal(Constant(tp.typeSymbol.name.decoded))).splice)
        val coll = c.Expr[Seq[Any]](target).splice
        coll foreach { ele =>
          c.Expr(buildTpe(pTpe, Ident(newTermName("ele")))).splice
        }
        formatterStack.splice.endArray()
      }
    }

    def writeMap(tp: Type, target: Tree): c.Expr[Unit] = {
      val TypeRef(_, _, keyTpe :: valTpe :: Nil) = tp
      reify {
        sw.splice.write(c.Expr[String](Literal(Constant(tp.typeSymbol.name.decoded))).splice)
        sw.splice.write('(')
        formatterStack.splice.startObject(tp.typeSymbol.name.decoded)
        c.Expr[scala.collection.GenMap[_, _]](target).splice.foreach { case (k, v) =>
          c.Expr(buildTpe(keyTpe, Ident(newTermName("k")))).splice
          sw.splice.write(' ')
          sw.splice.write('-')
          sw.splice.write('>')
          sw.splice.write(' ')
          c.Expr(buildTpe(valTpe, Ident(newTermName("v")))).splice
        }
        formatterStack.splice.endObject()
      }
    }

    def complexObject(tp: Type, target: Tree): c.Tree = {
      val TypeRef(_, sym: Symbol, tpeArgs: List[Type]) = tp.normalize
      val fields = helper.vals(tp) ++ helper.vars(tp)

      val fieldTrees = fields flatMap { pSym =>
        val tt = pSym.typeSignatureIn(tp)
        val fieldName = pSym.name.decoded.trim
        val fieldPath = Select(target, newTermName(fieldName))

        val startFieldExpr = reify {
          formatterStack.splice.startField(c.Expr[String](Literal(Constant(fieldName))).splice)
        }
        val fval = buildTpe(tt, fieldPath)
        fval :: startFieldExpr.tree :: Nil
      }
      Block(
        reify(formatterStack.splice.startObject(c.Expr[String](Literal(Constant(tp.typeSymbol.name.decoded))).splice)).tree ::
          fieldTrees.toList.reverse :::
          reify(formatterStack.splice.endObject()).tree ::
          Nil, EmptyTree)
    }

    def buildTpe(tp: Type, target: Tree): Tree = {
      primitiveTypes.find(_._1 =:= tp).map(_._2(target).tree) orElse {
        if (tp <:< collTpe) Some(writeList(tp, target).tree) else None
      } orElse {
        if (tp <:< mapTpe) Some(writeMap(tp, target).tree) else None
      } getOrElse {
        complexObject(tp, target)
      }
    }

    val tpe = weakTypeOf[T].normalize

    val Block(formatableClass :: Nil, _) = {
      reify {
        final class $anon extends Producer[T] {
          def writeFormatted[R](value: T, outputFormat: OutputFormat[R]): R = {
            val sw = new java.io.StringWriter()
            try {
              val formatter = outputFormat.createFormatter
              c.Expr(buildTpe(tpe, Ident(newTermName("value")))).splice
              formatter.result
            } finally {
              sw.close()
            }
          }
        }
      }.tree
    }

    c.Expr[Producer[T]](
      Block(
        List(formatableClass),
        Apply(Select(New(Ident(newTypeName("$anon"))), nme.CONSTRUCTOR), List())
      )
    )
  }
}