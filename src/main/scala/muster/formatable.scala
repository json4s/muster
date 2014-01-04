package muster

import scala.language.experimental.macros
import scala.reflect.macros._
import java.util.Date
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.MissingNode

trait Muster[T] {
  def writeFormatted[R](value: T, outputFormat: OutputFormat[R]): R
}

object Muster {

  implicit class FormattableProduct[T <: Product](p: T) {
    def writeFormatted[R](outputFormat: OutputFormat[R])(implicit fmt: Muster[T]): R =
      fmt.writeFormatted(p, outputFormat)
  }
  
  object into {
    object String extends DefaultStringFormat
    object CompactJsonString extends JsonOutput
  }

  object from {
     object JsonString extends JacksonInputFormat[String] {
      def createCursor(in: String): Cursor = new JacksonInputCursor[String] {
        val source = in
        protected val node: JsonNode = try {
          mapper.readTree(source)
        } catch {
          case _: Throwable => MissingNode.getInstance()
        }
      }
    }
    object JsonStream extends JacksonInputFormat[java.io.InputStream] {
      def createCursor(in: java.io.InputStream): Cursor = new JacksonInputCursor[java.io.InputStream] {
        val source: java.io.InputStream = in
        protected val node: JsonNode = mapper.readTree(source)
      }
    }
    object JsonReader extends JacksonInputFormat[java.io.Reader] {
      def createCursor(in: java.io.Reader): Cursor = new JacksonInputCursor[java.io.Reader] {
        val source: java.io.Reader = in
        protected val node: JsonNode = mapper.readTree(source)
      }
    }
    object JsonFile extends JacksonInputFormat[java.io.File] {
      def createCursor(in: java.io.File): Cursor = new JacksonInputCursor[java.io.File] {
        val source: java.io.File = in
        protected val node: JsonNode = mapper.readTree(source)
      }
    }
    object JsonByteArray extends JacksonInputFormat[Array[Byte]] {
      def createCursor(in: Array[Byte]): Cursor = new JacksonInputCursor[Array[Byte]] {
        val source: Array[Byte] = in
        protected val node: JsonNode = mapper.readTree(source)
      }
    }
    object JsonURL extends JacksonInputFormat[java.net.URL] {
      def createCursor(in: java.net.URL): Cursor = new JacksonInputCursor[java.net.URL] {
        val source: java.net.URL = in
        protected val node: JsonNode = mapper.readTree(source)
      }
    }
  }

  implicit def formatable[T]: Muster[T] = macro formatableImpl[T]

  def formatableImpl[T:c.WeakTypeTag](c: Context): c.Expr[Muster[T]] = {
    import c.universe._
    import definitions._
    import Flag._
    val helper = new Helper[c.type](c)

    val sw = c.Expr[java.io.StringWriter](Ident(newTermName("sw")))
    val formatterStack = c.Expr[OutputFormatter[_]](Ident(newTermName("formatter")))

    val primitiveTypes =
      Vector(
        (typeOf[Int], (t: Tree) => reify{formatterStack.splice.int(c.Expr[Int](t).splice)}),
        (typeOf[String], (t: Tree) => reify{formatterStack.splice.string(c.Expr[String](t).splice)}),
        (typeOf[Float], (t: Tree) => reify{formatterStack.splice.float(c.Expr[Float](t).splice)}),
        (typeOf[Double], (t: Tree) => reify{formatterStack.splice.double(c.Expr[Double](t).splice)}),
        (typeOf[Boolean], (t: Tree) => reify{formatterStack.splice.boolean(c.Expr[Boolean](t).splice)}),
        (typeOf[Long], (t: Tree) => reify{formatterStack.splice.long(c.Expr[Long](t).splice)}),
        (typeOf[Byte], (t: Tree) => reify{formatterStack.splice.byte(c.Expr[Byte](t).splice)}),
        (typeOf[BigInt], (t: Tree) => reify{formatterStack.splice.bigInt(c.Expr[BigInt](t).splice)}),
        (typeOf[Short], (t: Tree) => reify{formatterStack.splice.short(c.Expr[Short](t).splice)}),
        (typeOf[BigDecimal], (t: Tree) => reify{formatterStack.splice.bigDecimal(c.Expr[BigDecimal](t).splice)}),
        (typeOf[java.lang.Integer], (t: Tree) => reify{formatterStack.splice.int(c.Expr[Int](t).splice)}),
        (typeOf[java.lang.String], (t: Tree) => reify{formatterStack.splice.string(c.Expr[String](t).splice)}),
        (typeOf[java.lang.Float], (t: Tree) => reify{formatterStack.splice.float(c.Expr[Float](t).splice)}),
        (typeOf[java.lang.Double], (t: Tree) => reify{formatterStack.splice.double(c.Expr[Double](t).splice)}),
        (typeOf[java.lang.Boolean], (t: Tree) => reify{formatterStack.splice.boolean(c.Expr[Boolean](t).splice)}),
        (typeOf[java.lang.Long], (t: Tree) => reify{formatterStack.splice.long(c.Expr[Long](t).splice)}),
        (typeOf[java.lang.Byte], (t: Tree) => reify{formatterStack.splice.byte(c.Expr[Byte](t).splice)}),
        (typeOf[java.math.BigInteger], (t: Tree) => reify{formatterStack.splice.bigInt(c.Expr[BigInt](t).splice)}),
        (typeOf[java.lang.Short], (t: Tree) => reify{formatterStack.splice.short(c.Expr[Short](t).splice)}),
        (typeOf[java.math.BigDecimal], (t: Tree) => reify{formatterStack.splice.bigDecimal(c.Expr[BigDecimal](t).splice)}),
        (typeOf[Date], (t: Tree) => reify{formatterStack.splice.date(c.Expr[Date](t).splice)}),
        (typeOf[scala.Symbol], (t: Tree) => reify{formatterStack.splice.string(c.Expr[scala.Symbol](t).splice.name)}))

    val collTpe = typeOf[scala.collection.GenSeq[_]]
    val mapTpe = typeOf[scala.collection.GenMap[_, _]]

    def writeList(tp: Type, target: Tree): c.Expr[Unit] = {
      val TypeRef(_, _: Symbol, pTpe::Nil) = tp
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
      val TypeRef(_, _, keyTpe::valTpe::Nil) = tp
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

        val startFieldExpr =  reify{formatterStack.splice.startField(c.Expr[String](Literal(Constant(fieldName))).splice)}
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

    val Block(formatableClass::Nil, _) = {
      reify {
        final class $anon extends Muster[T] {
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

    c.Expr[Muster[T]](
      Block(
       List(formatableClass),
       Apply(Select(New(Ident(newTypeName("$anon"))), nme.CONSTRUCTOR), List())
      )
    )
  }
}