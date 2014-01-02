package muster

import scala.language.experimental.macros
import scala.reflect.macros._
import java.util.Date
import org.joda.time.DateTime
import scala.collection.{GenMap, mutable, generic}

trait Readable[S] {
  def readFormatted[R](source: R, inputFormat: InputFormat[R]): S
}

object Readable {

  implicit def readable[T]: Readable[T] = macro readableImpl[T]

  def readableImpl[T: c.WeakTypeTag](c: Context): c.Expr[Readable[T]] = {
    import c.universe._
    import definitions._
    import Flag._
    val helper = new Helper[c.type](c)

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
        typeOf[java.util.Date] -> "Date",
        typeOf[java.sql.Timestamp] -> "Date"
      ).withDefault(_.typeSymbol.name.decoded)

    val thisType = weakTypeOf[T]
    val collTpe = weakTypeOf[Seq[T]]
    val thisSymbol = thisType.typeSymbol
    //     val companionSymbol = thisSymbol.companionSymbol
    //     val companionType = companionSymbol.typeSignature

    //     val cursor = c.Expr[InputCursor[_]](Ident(newTermName("cursor")))
    val inputFormat = c.Expr[InputFormat[_]](Ident(newTermName("inputFormat")))

    def buildPrimitive(tpe: Type, cursor: c.Expr[Any], methodNameSuffix: String = "", args: List[Tree] = Nil): Tree = {
      Apply(Select(cursor.tree, newTermName(s"read${primitiveMap(tpe)}${methodNameSuffix}Value")), args)
    }

    def buildMap[TT](tpe: Type, reader: c.Expr[Any], suffix: String = "", args: List[Tree] = Nil): Tree = {
      val TypeRef(_, _, keyTpe :: valType :: Nil) = tpe
      val importExpr = c.parse(s"import ${tpe.normalize.typeConstructor.typeSymbol.fullName}")
      val bldrName = c.fresh("bldr$")

      val tupleType = appliedType(weakTypeOf[(Any, Any)].typeConstructor, keyTpe :: valType :: Nil)

      val bldrTree = ValDef(
        Modifiers(),
        newTermName(bldrName),
        AppliedTypeTree(Ident(weakTypeOf[mutable.Builder[(Any, Any), TT]].typeSymbol), List(TypeTree(tupleType), TypeTree(tpe))),
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
        while (iter.hasNext) {
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
      val TypeRef(_, _, argTpe :: Nil) = tpe
      val pref = if (methodSuffix == "Field") methodSuffix else ""
      buildSingle(argTpe, reader, s"${pref}Opt", args)
    }



    def buildObject(_tpe: Type, reader: c.Expr[Any], methodSuffix: String = "", args: List[Tree] = Nil): Tree = {
      val tpe = _tpe.normalize
      if (helper.isCaseClass(tpe)) {
        val TypeRef(_, sym, tpeArgs) = tpe
        val newObjTypeTree = helper.typeArgumentTree(tpe)

        val cn = c.fresh("objReader$")
        val ce = c.Expr[Ast.ObjectNode](Ident(newTermName(cn)))
        val ct: Tree = ValDef(
          Modifiers(),
          newTermName(cn),
          TypeTree(weakTypeOf[Ast.ObjectNode]),
          Apply(Select(reader.tree, newTermName(s"readObject$methodSuffix")), args)
        )

        // Builds the if/else tree for checking constructor params and returning a new object
        def pickConstructorTree(argNames: c.Expr[Set[String]]): Tree = {
          // Makes expressions for determining of they list is satisfied by the reader
          def ctorCheckingExpr(ctors: List[List[Symbol]]): c.Expr[Boolean] = {
            def isRequired(item: Symbol) = {
              val sym = item.asTerm
              !(sym.isParamWithDefault || sym.typeSignature <:< typeOf[Option[_]])
            }

            val expr = c.Expr[Set[String]](Apply(Select(Ident("Set"), newTermName("apply")),
              ctors.flatten.filter(isRequired).map(sym => Literal(Constant(sym.name.decoded)))
            ))

            reify(expr.splice.subsetOf(argNames.splice))
          }

          def ifElseTreeBuilder(ctorSets: List[(c.Expr[Boolean], List[List[Symbol]])]): Tree = ctorSets match {
            case h :: Nil => buildObjFromParams(h._2)
            case h :: t => If(h._1.tree, buildObjFromParams(h._2), ifElseTreeBuilder(t))
          }



          val ctors: List[MethodSymbol] = tpe.member(nme.CONSTRUCTOR)
            .asTerm.alternatives // List of constructors
            .map(_.asMethod) // method symbols
            .sortBy(-_.paramss.flatten.size)
          val ifExprsAndParams = ctors.map(ctor => ctorCheckingExpr(ctor.paramss)).zip(ctors.map(_.asMethod.paramss))

          ifElseTreeBuilder(ifExprsAndParams)
        }

        def writeListParams()

        def buildObjFromParams(ctorParams: List[List[Symbol]]): Tree = {

          New(newObjTypeTree, ctorParams.map(_.zipWithIndex.map {
            case (pSym, index) =>
              // Change out the types if it has type parameters
              val pTpe = pSym.typeSignature.substituteTypes(sym.asClass.typeParams, tpeArgs)
              val fieldName = Literal(Constant(pSym.name.decoded))

              // If param has defaults, try to find the val in map, or call
              // default evaluation from its companion object
              // TODO: is the sym.companionSymbol.isTerm the best way to check for NoSymbol?
              // TODO: is there a way to get teh default values for the overloaded constructors?
              if (pSym.asTerm.isParamWithDefault && helper.isPrimitive(pTpe) && sym.companionSymbol.isTerm) {
                reify {
                  c.Expr[Option[Any]](buildOption(pTpe, ce, "Field", List(fieldName))).splice
                    .getOrElse(c.Expr(Select(
                                        Ident(sym.companionSymbol),
                                        newTermName("$lessinit$greater$default$" + (index + 1).toString))).splice)
                }.tree
              } else if (pSym.asTerm.isParamWithDefault && sym.companionSymbol.isTerm) {
                reify {
                  try {
                    c.Expr(buildSingle(pTpe, ce, "Field", List(fieldName))).splice // splice in another obj tree
                  } catch {
                    case e: MappingException =>
                      // Need to use the origional symbol.companionObj to get defaults
                      // Would be better to find the generated TermNames if possible
                      c.Expr(Select(Ident(sym.companionSymbol), newTermName(
                        "$lessinit$greater$default$" + (index + 1).toString))
                      ).splice
                  }
                }.tree
              } else buildSingle(pTpe, ce, "Field", List(fieldName))
          }))
        }

        val on = c.fresh("obj$")
        val ot = newTermName(on)
        val oe = c.Expr(Ident(ot))
        val otr: Tree = ValDef(Modifiers(), ot, newObjTypeTree, pickConstructorTree(reify(ce.splice.keySet)))

        // Sets fields after the instance is has been created
        def optionalParams(pTpe: Type, varName: String, exprMaker: Tree => c.Expr[_]): Tree = {
          val compName = Literal(Constant(varName))
          // Use option if primitive, should be faster than exceptions.
          reify {
            c.Expr[Option[Any]](buildOption(pTpe, ce, "Field", List(compName))).splice match {
              case Some(x) => exprMaker(Ident(newTermName("x"))).splice
              case None =>
            }
          }.tree
        }

        val setVarsBlocks: List[Tree] =
          helper.getNonConstructorVars(tpe).toList.map {
            pSym =>
              val varName = pSym.name.toTermName.toString.trim
              val tpe = pSym.typeSignature.substituteTypes(sym.asClass.typeParams, tpeArgs)
              optionalParams(tpe, varName,
                tree => c.Expr(Assign(Select(Ident(ot), newTermName(varName)), tree))
              )
          }

        val setSetterBlocks: List[Tree] =
          helper.getJavaStyleSetters(tpe).toList.map {
            pSym => // MethodSymbol
              val origName = pSym.name.decoded.substring(3)
              val name = origName.charAt(0).toLower + origName.substring(1)
              val paramType = {
                val tpe = pSym.asMethod.paramss(0)(0)
                tpe.typeSignature.substituteTypes(sym.asClass.typeParams, tpeArgs)
              }
              optionalParams(paramType, name,
                tree => c.Expr(Apply(Select(Ident(ot), pSym.name), tree :: Nil))
              )
          }

        Block(ct :: otr :: setVarsBlocks ::: setSetterBlocks, Ident(ot))

      } else {
        c.abort(c.enclosingPosition, "Only case classes with a single constructor are supported")
      }

    }

    def buildSingle(tpe: Type, reader: c.Expr[Any], methodSuffix: String = "", args: List[Tree] = Nil): Tree = {
      if (helper.isPrimitive(tpe)) {
        buildPrimitive(tpe, reader, methodSuffix, args)
      } else if (helper.isOption(tpe))
        buildOption(tpe, reader, methodSuffix, args)
      else if (helper.isSeq(tpe))
        buildCollection[scala.collection.GenSeq[Any]](tpe, reader, methodSuffix, args)
      else if (helper.isSet(tpe))
        buildCollection[scala.collection.GenSet[Any]](tpe, reader, methodSuffix, args)
      else if (helper.isMap(tpe))
        buildMap[scala.collection.GenMap[Any, Any]](tpe, reader, methodSuffix, args)
      else if (helper.isCaseClass(tpe))
        buildObject(tpe, reader, methodSuffix, args)
      else {
        c.abort(c.enclosingPosition, s"$tpe is not supported currently")
      }
    }


    val importExpr = c.parse(s"import ${thisType.normalize.typeConstructor.typeSymbol.fullName}")
    reify {
      new Readable[T] {
        c.Expr(importExpr).splice

        def readFormatted[R](source: R, inputFormat: InputFormat[R]): T = {
          val cursor = inputFormat.createCursor(source)
          c.Expr(buildSingle(thisType, c.Expr[AstCursor](Ident(newTermName("cursor"))))).splice
        }
      }
    }
  }
}

