package muster

import scala.language.experimental.macros
import scala.reflect.macros._
import java.util.Date
import org.joda.time.DateTime
import scala.collection.{GenMap, mutable, generic}

trait Readable[S] {
  def readFormated[R](source: R, inputFormat: InputFormat[R]): S
}

object Readable {

  implicit def readable[T]: Readable[T] = macro readableImpl[T]

  def readableImpl[T: c.WeakTypeTag](c: Context): c.Expr[Readable[T]] = {
    import c.universe._
    import definitions._
    import Flag._
    val helper = new Helper[c.type](c)



    val thisType = weakTypeOf[T]
    val collTpe = weakTypeOf[Seq[T]]
    val thisSymbol = thisType.typeSymbol
    //     val companionSymbol = thisSymbol.companionSymbol
    //     val companionType = companionSymbol.typeSignature

    //     val cursor = c.Expr[InputCursor[_]](Ident(newTermName("cursor")))
    val inputFormat = c.Expr[InputFormat[_]](Ident(newTermName("inputFormat")))

    val primitiveMap =
      Map[Type, String](
        typeOf[java.math.BigDecimal] -> "JBigDecimal",
        typeOf[java.lang.Byte] -> "JByte",
        typeOf[java.lang.Short] -> "JShort",
        typeOf[java.lang.Long] -> "JLong",
        typeOf[java.lang.Float] -> "JFloat",
        typeOf[java.lang.Double] -> "JDouble",
        typeOf[java.util.Date] -> "String",
        typeOf[org.joda.time.DateTime] -> "DateTime",
        typeOf[java.util.Date] -> "Date"
      ).withDefault(_.typeSymbol.name.decoded)

    def buildPrimitive(tpe: Type, cursor: c.Expr[Any], methodNameSuffix: String = "", args: List[Tree] = Nil): Tree = {
      Apply(Select(cursor.tree, newTermName(s"read${primitiveMap(tpe)}${methodNameSuffix}Value")), args)
    }

    def buildMap[TT](tpe: Type, reader: c.Expr[Any], suffix: String = "", args: List[Tree] = Nil): Tree = {
      val TypeRef(_, _, keyTpe :: valType :: Nil) = tpe
      val importExpr = c.parse(s"import ${tpe.normalize.typeConstructor.typeSymbol.fullName}")
      val bldrName = c.fresh("bldr$")

      val tupleType = appliedType(weakTypeOf[(Any, Any)].typeConstructor, keyTpe::valType::Nil)

      val bldrTree = ValDef(
        Modifiers(),
        newTermName(bldrName),
        AppliedTypeTree(Ident(weakTypeOf[mutable.Builder[(Any, Any), Map[Any, Any]]].typeSymbol), List(TypeTree(tupleType), TypeTree(tpe))),
        TypeApply(Select(Ident(newTermName(tpe.typeSymbol.name.decoded)), newTermName("newBuilder")), List(TypeTree(keyTpe), TypeTree(valType)))
      )
      val builder = c.Expr[mutable.Builder[(Any, Any), Map[Any, Any]]](Ident(newTermName(bldrName)))
      val cursorName = c.fresh("dict$")
      val cursorExpr = c.Expr[Ast.ObjectNode](Ident(newTermName(cursorName)))
      val cursorTree = ValDef(
        Modifiers(),
        newTermName(cursorName),
        TypeTree(weakTypeOf[Ast.ObjectNode]),
        Apply(Select(reader.tree, newTermName(s"readObject$suffix")), args)
      )


      val readFieldTree: Tree = {
        val keyExpr = c.Expr[String](Ident(newTermName("k")))
        val suff = if (suffix == "Opt") suffix else ""
        buildSingle(valType, cursorExpr, s"Field$suff", List(keyExpr.tree))
      }

      reify {
        c.Expr(importExpr).splice
        c.Expr(bldrTree).splice
        c.Expr(cursorTree).splice
        val iter = cursorExpr.splice.keysIterator
        while(iter.hasNext) {
          val k = iter.next()
          val v = c.Expr[Any](readFieldTree).splice
          builder.splice += (k -> v)
        }
        builder.splice.result()
      }.tree
    }

    def buildCollection[TT](tpe: Type, reader: c.Expr[Any], suffix: String = "", args: List[Tree] = Nil): Tree = {
      val TypeRef(_, _, argTpe :: Nil) = tpe
      val importExpr = c.parse(s"import ${tpe.normalize.typeConstructor.typeSymbol.fullName}")
      val builderExpr = c.Expr[collection.mutable.Builder[Any, TT]](
        TypeApply(Select(Ident(newTermName(tpe.typeSymbol.name.decoded)), newTermName("newBuilder")), List(TypeTree(argTpe)))
      )
      val itName = c.fresh("arr$")
      val itExpr = c.Expr[AstCursor](Ident(newTermName(itName)))
      val itTree = ValDef(
        Modifiers(),
        newTermName(itName),
        TypeTree(typeOf[AstCursor]),
        Apply(Select(reader.tree, newTermName(s"readArray$suffix")), args)
      )

      reify {
        c.Expr(importExpr).splice
        val builder = builderExpr.splice
        c.Expr(itTree).splice
        while (itExpr.splice.hasNextNode) {
          builder += c.Expr[Any](buildSingle(argTpe, itExpr)).splice
        }
        builder.result()
      }.tree
    }

    def buildOption(tpe: Type, reader: c.Expr[Any], methodSuffix: String = "", args: List[Tree] = Nil): Tree = {
      val TypeRef(_, _, argTpe::Nil) = tpe
      val pref = if (methodSuffix == "Field") methodSuffix else ""
      buildSingle(argTpe, reader, s"${pref}Opt", args)
    }

    def buildSingle(tpe: Type, reader: c.Expr[Any], methodSuffix: String = "", args: List[Tree] = Nil): Tree = {
      if (helper.isPrimitive(tpe)) {
        buildPrimitive(tpe, reader, methodSuffix, args)
      } else if (tpe <:< weakTypeOf[scala.Option[_]])
        buildOption(tpe, reader, methodSuffix, args)
      else if (tpe <:< weakTypeOf[scala.collection.GenSeq[_]])
        buildCollection[scala.collection.GenSeq[Any]](tpe, reader, methodSuffix, args)
      else if (tpe <:< weakTypeOf[scala.collection.GenSet[_]])
        buildCollection[scala.collection.GenSet[Any]](tpe, reader, methodSuffix, args)
      else if (tpe <:< weakTypeOf[scala.collection.GenMap[_, _]])
        buildMap[scala.collection.GenMap[Any, Any]](tpe, reader, methodSuffix, args)
      else {
        c.abort(c.enclosingPosition, s"$tpe is not supported currently")
      }
    }

    val importExpr = c.parse(s"import ${thisType.normalize.typeConstructor.typeSymbol.fullName}")
    reify {
      new Readable[T] {
        c.Expr(importExpr).splice
        def readFormated[R](source: R, inputFormat: InputFormat[R]): T = {
          val cursor = inputFormat.createCursor(source)
          c.Expr(buildSingle(thisType, c.Expr[AstCursor](Ident(newTermName("cursor"))))).splice
        }
      }
    }
  }
}
