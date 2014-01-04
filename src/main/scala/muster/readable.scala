//package muster
//
//import scala.language.experimental.macros
//import scala.reflect.macros._
//import scala.collection.mutable
//
//
//trait Readable[S] {
//
//  def readFormatted[R](source: R, inputFormat: InputFormat[R]): S
//}
//
//object Readable {
//
//  implicit def readable[T]: Readable[T] = macro readableImpl[T]
//
//  def readableImpl[T: c.WeakTypeTag](c: Context): c.Expr[Readable[T]] = {
//    import c.universe._
//    val helper = new Helper[c.type](c)
//    import definitions._
//    import Flag._
//
//    val inputFormat = c.Expr[InputFormat[_]](Ident(newTermName("inputFormat")))
//    val dateFormatTree = reify(inputFormat.splice.dateFormat).tree
//    val primitiveMap =
//      Map[Type, (String, List[Tree])](
//        typeOf[java.math.BigDecimal] -> ("JBigDecimal", Nil),
//        typeOf[java.lang.Byte] -> ("JByte", Nil),
//        typeOf[java.lang.Short] -> ("JShort", Nil),
//        typeOf[java.lang.Long] -> ("JLong", Nil),
//        typeOf[java.lang.Float] -> ("JFloat", Nil),
//        typeOf[java.lang.Double] -> ("JDouble", Nil),
//        typeOf[java.util.Date] -> ("String", dateFormatTree :: Nil),
//        typeOf[java.sql.Timestamp] -> ("Date", dateFormatTree :: Nil)
//      ).withDefault(v => (v.typeSymbol.name.decoded, Nil))
//
//    val thisType = weakTypeOf[T].normalize
//    val collTpe = weakTypeOf[Seq[T]]
//    val thisSymbol = thisType.typeSymbol
//    //     val companionSymbol = thisSymbol.companionSymbol
//    //     val companionType = companionSymbol.typeSignature
//
//    //     val cursor = c.Expr[InputCursor[_]](Ident(newTermName("cursor")))
//    def buildPrimitive(tpe: Type, cursor: c.Expr[Any], methodNameSuffix: String = "", args: List[Tree] = Nil): Tree = {
//      val (nm, ags) = primitiveMap(tpe)
//      Apply(Select(cursor.tree, newTermName(s"read${nm}${methodNameSuffix}Value")), args ::: ags)
//    }
//
//    def buildMap[TT](tpe: Type, reader: c.Expr[Any], suffix: String = "", args: List[Tree] = Nil): Tree = {
//      val TypeRef(_, _, keyTpe :: valType :: Nil) = tpe
//      val importExpr = c.parse(s"import ${tpe.normalize.typeConstructor.typeSymbol.fullName}")
//      val bldrName = c.fresh("bldr$")
//
//      val tupleType = appliedType(weakTypeOf[(Any, Any)].typeConstructor, keyTpe :: valType :: Nil)
//
//      val bldrTree = ValDef(
//        Modifiers(),
//        newTermName(bldrName),
//        AppliedTypeTree(Ident(weakTypeOf[mutable.Builder[(Any, Any), TT]].typeSymbol), List(TypeTree(tupleType), TypeTree(tpe))),
//        TypeApply(Select(Ident(newTermName(tpe.typeSymbol.name.decoded)), newTermName("newBuilder")), List(TypeTree(keyTpe), TypeTree(valType)))
//      )
//      val builder = c.Expr[mutable.Builder[(Any, Any), Map[Any, Any]]](Ident(newTermName(bldrName)))
//      val cursorName = c.fresh("dict$")
//      val cursorExpr = c.Expr[Ast.ObjectNode](Ident(newTermName(cursorName)))
//      val cursorTree = ValDef(
//        Modifiers(),
//        newTermName(cursorName),
//        TypeTree(weakTypeOf[Ast.ObjectNode]),
//        Apply(Select(reader.tree, newTermName(s"readObject$suffix")), args)
//      )
//
//
//      val readFieldTree: Tree = {
//        val keyExpr = c.Expr[String](Ident(newTermName("k")))
//        val suff = if (suffix == "Opt") suffix else ""
//        buildSingle(valType, cursorExpr, s"Field$suff", List(keyExpr.tree))
//      }
//
//      reify {
//        c.Expr(importExpr).splice
//        c.Expr(bldrTree).splice
//        c.Expr(cursorTree).splice
//        val iter = cursorExpr.splice.keysIterator
//        while (iter.hasNext) {
//          val k = iter.next()
//          val v = c.Expr[Any](readFieldTree).splice
//          builder.splice += (k -> v)
//        }
//        builder.splice.result()
//      }.tree
//    }
//
//    def buildCollection[TT](tpe: Type, reader: c.Expr[Any], suffix: String = "", args: List[Tree] = Nil): Tree = {
//      val TypeRef(_, _, argTpe :: Nil) = tpe
//      val importExpr = c.parse(s"import ${tpe.normalize.typeConstructor.typeSymbol.fullName}")
//      val builderExpr = c.Expr[collection.mutable.Builder[Any, TT]](
//        TypeApply(Select(Ident(newTermName(tpe.typeSymbol.name.decoded)), newTermName("newBuilder")), List(TypeTree(argTpe)))
//      )
//      val itName = c.fresh("arr$")
//      val itExpr = c.Expr[AstCursor](Ident(newTermName(itName)))
//      val itTree = ValDef(
//        Modifiers(),
//        newTermName(itName),
//        TypeTree(typeOf[AstCursor]),
//        Apply(Select(reader.tree, newTermName(s"readArray$suffix")), args)
//      )
//
//      reify {
//        c.Expr(importExpr).splice
//        val builder = builderExpr.splice
//        c.Expr(itTree).splice
//        while (itExpr.splice.hasNextNode) {
//          builder += c.Expr[Any](buildSingle(argTpe, itExpr)).splice
//        }
//        builder.result()
//      }.tree
//    }
//
//    def buildOption(tpe: Type, reader: c.Expr[Any], methodSuffix: String = "", args: List[Tree] = Nil): Tree = {
//      val TypeRef(_, _, argTpe :: Nil) = tpe
//      val pref = if (methodSuffix startsWith "Field") methodSuffix else ""
//      val suff = if (methodSuffix endsWith "Opt") "" else "Opt"
//      val tree = buildSingle(argTpe, reader, s"$pref$suff", args)
//      if (methodSuffix contains ("Opt"))
//        reify(Option(c.Expr(tree).splice)).tree
//      else tree
//    }
//
//
//
//    def buildObject(tpe: Type, reader: c.Expr[Any], methodSuffix: String = "", args: List[Tree] = Nil): Tree = {
//      if (helper.isCaseClass(tpe)) {
//        val TypeRef(_, sym, tpeArgs) = tpe
//        val newObjTypeTree = helper.typeArgumentTree(tpe)
//
//        val cn = c.fresh("objReader$")
//        val ce = c.Expr[Ast.ObjectNode](Ident(newTermName(cn)))
//        val ct: Tree = ValDef(
//          Modifiers(),
//          newTermName(cn),
//          TypeTree(weakTypeOf[Ast.ObjectNode]),
//          Apply(Select(reader.tree, newTermName(s"readObject$methodSuffix")), args)
//        )
//
//        // Builds the if/else tree for checking constructor params and returning a new object
//        def pickConstructorTree(argNames: c.Expr[Set[String]]): Tree = {
//          // Makes expressions for determining of they list is satisfied by the reader
//          def ctorCheckingExpr(ctors: List[List[Symbol]]): c.Expr[Boolean] = {
//             def isRequired(item: Symbol) = {
//              val sym = item.asTerm
//              !(sym.isParamWithDefault || sym.typeSignature <:< typeOf[Option[_]])
//            }
//
//            val expr = c.Expr[Set[String]](Apply(Select(Ident(newTermName("Set")), newTermName("apply")),
//              ctors.flatten.filter(isRequired).map(sym => Literal(Constant(sym.name.decoded)))
//            ))
//
//            reify(expr.splice.subsetOf(argNames.splice))
//          }
//
//          def ifElseTreeBuilder(ctorSets: List[(c.Expr[Boolean], List[List[Symbol]])]): Tree = ctorSets match {
//            case h :: Nil => buildObjFromParams(h._2)
//            case h :: t => If(h._1.tree, buildObjFromParams(h._2), ifElseTreeBuilder(t))
//          }
//
//
//
//          val ctors: List[MethodSymbol] = tpe.member(nme.CONSTRUCTOR)
//            .asTerm.alternatives // List of constructors
//            .map(_.asMethod) // method symbols
//            .sortBy(-_.paramss.flatten.size)
//          val ifExprsAndParams = ctors.map(ctor => ctorCheckingExpr(ctor.paramss)).zip(ctors.map(_.asMethod.paramss))
//
//          ifElseTreeBuilder(ifExprsAndParams)
//        }
//
////        def writeListParams()
//
//        def buildObjFromParams(ctorParams: List[List[Symbol]]): Tree = {
//          val params = ctorParams.map(_.zipWithIndex.map {
//            case (pSym, index) =>
//              // Change out the types if it has type parameters
//              val pTpe = pSym.typeSignature.substituteTypes(sym.asClass.typeParams, tpeArgs)
//              val fieldName = Literal(Constant(pSym.name.decoded))
//              val pTnm = newTermName(pSym.name.decoded)
//
//              // If param has defaults, try to find the val in map, or call
//              // default evaluation from its companion object
//              // TODO: is the sym.companionSymbol.isTerm the best way to check for NoSymbol?
//              // TODO: is there a way to get teh default values for the overloaded constructors?
//              val tree = if (pSym.asTerm.isParamWithDefault && sym.companionSymbol.isTerm) {
//                reify {
//                  c.Expr[Option[Any]](buildOption(pTpe, ce, "Field", List(fieldName))).splice
//                    .getOrElse(c.Expr(Select(
//                                        Ident(sym.companionSymbol),
//                                        newTermName("$lessinit$greater$default$" + (index + 1).toString))).splice)
//                }.tree
//              } else buildSingle(pTpe, ce, "Field", List(fieldName))
//              (ValDef(Modifiers(), pTnm, TypeTree(pTpe), tree), Ident(pTnm))
//          })
//
//          Block(params.head.map(_._1), Apply(Select(New(Ident(sym)), nme.CONSTRUCTOR), params.head.map(_._2)))
//        }
//
//        val on = c.fresh("obj$")
//        val ot = newTermName(on)
//        val otr: Tree = ValDef(Modifiers(), ot, newObjTypeTree, pickConstructorTree(reify(ce.splice.keySet)))
//
//        // Sets fields after the instance is has been created
//        def optionalParams(pTpe: Type, varName: String, exprMaker: Tree => c.Expr[_]): Tree = {
//          val compName = Literal(Constant(varName))
//          // Use option if primitive, should be faster than exceptions.
//          reify {
//            c.Expr[Option[Any]](buildOption(pTpe, ce, "Field", List(compName))).splice match {
//              case Some(x) => exprMaker(Ident(newTermName("x"))).splice
//              case None =>
//            }
//          }.tree
//        }
//
//        val setVarsBlocks: List[Tree] =
//          helper.getNonConstructorVars(tpe).toList.map {
//            pSym =>
//              val varName = pSym.name.toTermName.toString.trim
//              val tpe = pSym.typeSignature.substituteTypes(sym.asClass.typeParams, tpeArgs)
//              optionalParams(tpe, varName,
//                tree => c.Expr(Assign(Select(Ident(ot), newTermName(varName)), tree))
//              )
//          }
//
//        val setSetterBlocks: List[Tree] =
//          helper.getJavaStyleSetters(tpe).toList.map {
//            pSym => // MethodSymbol
//              val origName = pSym.name.decoded.substring(3)
//              val name = origName.charAt(0).toLower + origName.substring(1)
//              val paramType = {
//                val tpe = pSym.asMethod.paramss(0)(0)
//                tpe.typeSignature.substituteTypes(sym.asClass.typeParams, tpeArgs)
//              }
//              optionalParams(paramType, name,
//                tree => c.Expr(Apply(Select(Ident(ot), pSym.name), tree :: Nil))
//              )
//          }
//
//        Block(ct :: otr :: setVarsBlocks ::: setSetterBlocks, Ident(ot))
//
//      } else {
//        c.abort(c.enclosingPosition, "Only case classes with a single constructor are supported")
//      }
//
//    }
//
//    def valueTree(tpe: Type, reader: c.Expr[Any], methodSuffix: String = "", args: List[Tree] = Nil): Tree = {
//      if (helper.isPrimitive(tpe)) {
//        buildPrimitive(tpe, reader, methodSuffix, args)
//      } else if (helper.isOption(tpe))
//        buildPrimitive(tpe, reader, methodSuffix, args)
//      else if (helper.isSeq(tpe))
//        buildCollection[scala.collection.GenSeq[Any]](tpe, reader, methodSuffix, args)
//      else if (helper.isSet(tpe))
//        buildCollection[scala.collection.GenSet[Any]](tpe, reader, methodSuffix, args)
//      else if (helper.isMap(tpe))
//        buildMap[scala.collection.GenMap[Any, Any]](tpe, reader, methodSuffix, args)
//      else if (helper.isCaseClass(tpe))
//        buildObject(tpe, reader, methodSuffix, args)
//      else {
//        c.abort(c.enclosingPosition, s"$tpe is not supported currently")
//      }
//    }
//
//    def buildSingle(_tpe: Type, reader: c.Expr[Any], methodSuffix: String = "", args: List[Tree] = Nil): Tree = {
//      val tpe = _tpe.normalize
//      val t = appliedType(weakTypeOf[Consumer[Any]].typeConstructor, tpe :: Nil)
//      valueTree(tpe, reader, methodSuffix, args)
////        case x =>
////          val fn = c.fresh("consumer$")
////          val vn = newTermName(fn)
////          val v = ValDef(Modifiers(), vn, TypeTree(t), x)
////
////          val cn = c.fresh("node$")
////          val rdrOpt: Tree = {
////            if (helper.isPrimitive(tpe)) {
////              val (nm, _) = primitiveMap(tpe)
////              Apply(Select(reader.tree, newTermName(s"read${nm}${methodSuffix}Opt")), args)
////            } else if (helper.isOption(tpe)) {
////              val TypeRef(_, _, agType :: Nil) = tpe
////              val (nm, _) = primitiveMap(agType)
////              Apply(Select(reader.tree, newTermName(s"read${nm}${methodSuffix}Opt")), args)
////            } else if (helper.isSeq(tpe) || helper.isSet(tpe))
////              Apply(Select(reader.tree, newTermName(s"readArray${methodSuffix}Opt")), args)
////            else
////              Apply(Select(reader.tree, newTermName(s"readObject${methodSuffix}Opt")), args)
////          }
////
////          val tree = Apply(Select(rdrOpt, newTermName("getOrElse")), Select(Ident(newTermName("muster.Ast")), newTermName("NullNode")) :: Nil)
////          val ce = c.Expr[Ast.AstNode[_]](Ident(newTermName(cn)))
////          val ct: Tree = ValDef(Modifiers(),newTermName(cn), TypeTree(weakTypeOf[Ast.AstNode[_]]), tree)
////          Block(v :: ct :: Nil, Apply(Select(Ident(vn), newTermName("consume")), ce.tree :: Nil))
////      }
//    }
//
//
//    val importExpr = c.parse(s"import ${thisType.normalize.typeConstructor.typeSymbol.fullName}")
//    reify {
//      new Readable[T] {
//        c.Expr(importExpr).splice
//
//        def readFormatted[R](source: R, inputFormat: InputFormat[R]): T = {
//          val cursor = inputFormat.createCursor(source)
//          c.Expr(buildSingle(thisType, c.Expr[AstCursor](Ident(newTermName("cursor"))))).splice
//        }
//      }
//    }
//  }
//}
//
