package muster

import scala.language.experimental.macros
import scala.reflect.macros._
import muster.Ast._
import scala.reflect.ClassTag
import java.util.Date
import java.text.SimpleDateFormat
import scala.collection.{generic, immutable}
import muster.Ast.TextNode
import muster.Ast.NumberNode
import scala.util.Try
import scala.annotation.tailrec

trait Consumer[S] {
  def consume(node: AstNode[_]): S
}

object Consumer {

  def nullValue = Ast.NullNode

  private def cc[S](fn: PartialFunction[AstNode[_], S])(implicit mf: ClassTag[S]) = new Consumer[S] {
    def consume(node: AstNode[_]): S = if (fn.isDefinedAt(node)) fn(node) else throw new MappingException(s"Couldn't convert $node to ${mf.runtimeClass.getSimpleName}")
  }

  private def nc[S](fn: NumberNodeLike[_] => S)(implicit mf: ClassTag[S]) = cc[S]{
    case m: NumberNodeLike[_] => fn(m)
    case m: TextNode => fn(NumberNode(m.value))
  }

  implicit val BooleanConsumer = cc[Boolean]({
    case m: BoolNode => m.value
    case NullNode | UndefinedNode => false
  })
  implicit val ByteConsumer = nc[Byte](_.toByte)
  implicit val ShortConsumer = nc[Short](_.toShort)
  implicit val IntConsumer = nc[Int](_.toInt)
  implicit val LongConsumer = nc[Long](_.toLong)
  implicit val BigIntConsumer = nc[BigInt](_.toBigInt)
  implicit val FloatConsumer = nc[Float](_.toFloat)
  implicit val DoubleConsumer = nc[Double](_.toDouble)
  implicit val BigDecimalConsumer = nc[BigDecimal](_.toBigDecimal)
  implicit val JavaByteConsumer = nc[java.lang.Byte](v => byte2Byte(v.toByte))
  implicit val JavaShortConsumer = nc[java.lang.Short](v => short2Short(v.toShort))
  implicit val JavaIntConsumer = nc[java.lang.Integer](v => int2Integer(v.toInt))
  implicit val JavaLongConsumer = nc[java.lang.Long](v => long2Long(v.toLong))
  implicit val JavaBigIntConsumer = nc[java.math.BigInteger](_.toBigInt.bigInteger)
  implicit val JavaFloatConsumer = nc[java.lang.Float](v => float2Float(v.toFloat))
  implicit val JavaDoubleConsumer = nc[java.lang.Double](v => double2Double(v.toDouble))
  implicit val JavaBigDecimalConsumer = nc[java.math.BigDecimal](_.toBigDecimal.bigDecimal)
  implicit val StringConsumer = cc[String] {
    case TextNode(value) => value
    case NumberNode(value) => value
    case m: NumberNodeLike[_] => m.value.toString
    case NullNode | UndefinedNode => null
  }

  implicit val SymbolConsumer = new Consumer[scala.Symbol] {
    def consume(node: AstNode[_]): Symbol = Symbol(StringConsumer.consume(node))
  }

  implicit val Iso8601DateConsumer = cc[Date]({
    case TextNode(value) => {
      SafeSimpleDateFormat.Iso8601Formatter.parse(value)
    }
    case m: NumberNodeLike[_] => new Date(m.toLong)
    case NullNode | UndefinedNode => null
  })

  def dateConsumer(pattern: String) = {
    cc[Date]({
      case TextNode(value) => new SimpleDateFormat(pattern).parse(value)
      case m: NumberNodeLike[_] => new Date(m.toLong)
      case NullNode | UndefinedNode => null
    })
  }


  implicit def mapConsumer[V](implicit valueConsumer: Consumer[V]) = cc[immutable.Map[String, V]] {
    case m: ObjectNode =>
      val bldr = Map.newBuilder[String, V]
      m.keySet foreach { key =>
        bldr += key -> valueConsumer.consume(m.readField(key))
      }
      bldr.result()
    case NullNode | UndefinedNode => null
  }

  import scala.language.higherKinds
  implicit def traversableReader[F[_], V](implicit cbf: generic.CanBuildFrom[F[_], V, F[V]], valueReader: Consumer[V]): Consumer[F[V]] =
    new Consumer[F[V]] {
      def consume(node: AstNode[_]): F[V] = node match {
        case m: ArrayNode =>
          val bldr = cbf()
          while(m.hasNextNode) {
            bldr += valueReader.consume(m.nextNode())
          }
          bldr.result()
        case x => throw new MappingException(s"Got a ${x.getClass.getSimpleName} and expected an ArrayNode")
      }
    }

  implicit def arrayConsumer[T](implicit ct: ClassTag[T], valueReader: Consumer[T]): Consumer[Array[T]] = {
    new Consumer[Array[T]] {
      def consume(node: AstNode[_]): Array[T] = node match {
        case m: ArrayNode =>
          val bldr = Array.newBuilder
          while(m.hasNextNode) {
            bldr += valueReader.consume(m.nextNode())
          }
          bldr.result()
        case x => throw new MappingException(s"Got a ${x.getClass.getSimpleName} and expected an ArrayNode")
      }
    }
  }

  implicit def optionConsumer[T](implicit valueReader: Consumer[T]): Consumer[Option[T]] = new Consumer[Option[T]] {
    def consume(node: AstNode[_]): Option[T] = node match {
      case NullNode | UndefinedNode => None
      case v => Try(valueReader.consume(v)).toOption
    }
  }

  implicit def consumer[T]: Consumer[T] = macro consumerImpl[T]

  def consumerImpl[T: c.WeakTypeTag](c: Context): c.Expr[Consumer[T]] = {
    import c.universe._
    import definitions._
    import Flag._

    val primitiveMap =
      Map[Type, String](
        typeOf[java.math.BigDecimal] -> "JBigDecimal",
        typeOf[java.lang.Byte] -> "JByte",
        typeOf[java.lang.Short] -> "JShort",
        typeOf[java.lang.Long] -> "JLong",
        typeOf[java.lang.Float] -> "JFloat",
        typeOf[java.lang.Double] -> "JDouble",
        typeOf[java.util.Date] -> "String",
        typeOf[java.sql.Timestamp] -> "String",
        typeOf[scala.Symbol] -> "String"
      ).withDefault(v => v.typeSymbol.name.decoded)


    val helper = new Helper[c.type](c)
    val thisType = weakTypeOf[T]

    val importExpr = c.parse(s"import ${thisType.normalize.typeConstructor.typeSymbol.fullName}")

//    def buildPrimitive(tpe: Type, cursor: c.Expr[Any], methodNameSuffix: String = "", args: List[Tree] = Nil): Tree = EmptyTree
//    def buildArray(tpe: Type, cursor: c.Expr[Any], methodNameSuffix: String = "", args: List[Tree] = Nil): Tree = EmptyTree

    val nullNodeDefault = reify(Ast.NullNode).tree
    def buildValue(_tpe: Type, reader: c.Expr[Any], methodSuffix: String = "", args: List[Tree] = Nil, default: Tree = nullNodeDefault): Tree = {
      val tpe = _tpe.normalize
      val t = appliedType(weakTypeOf[Consumer[Any]].typeConstructor, tpe :: Nil)
      c.inferImplicitValue(t) match {
        case EmptyTree => c.abort(c.enclosingPosition, s"Couldn't find a muster.Consumer[${t.typeSymbol.name.decoded}], try bringing an implicit value for ${tpe.typeSymbol.name.decoded} in scope by importing one or defining one.")
        case resolved =>
          val fn = c.fresh("consumer$")
          val vn = newTermName(fn)
          val v = ValDef(Modifiers(), vn, TypeTree(t), resolved)

          val cn = c.fresh("node$")
          val rdrOpt: Tree = {
            if (helper.isPrimitive(tpe)) {
              val nm = primitiveMap(tpe)
              Apply(Select(reader.tree, newTermName(s"read${nm}${methodSuffix}Opt")), args)
            } else if (helper.isOption(tpe)) {
              @tailrec def fetchType(tp: Type): Type = {
                if (tp <:< weakTypeOf[Option[_]]) {
                  val TypeRef(_, _, agTp :: Nil) = tp
                  fetchType(agTp)
                } else tp
              }
              val agType = fetchType(tpe)
              val nm = primitiveMap(agType)
              Apply(Select(reader.tree, newTermName(s"read${nm}${methodSuffix}Opt")), args)
            } else if (helper.isSeq(tpe) || helper.isSet(tpe))
              Apply(Select(reader.tree, newTermName(s"readArray${methodSuffix}Opt")), args)
            else
              Apply(Select(reader.tree, newTermName(s"readObject${methodSuffix}Opt")), args)
          }

          val tree = default match {
            case EmptyTree =>
              rdrOpt
            case x =>
              Apply(Select(rdrOpt, newTermName("getOrElse")), default :: Nil)
          }
          val ce = c.Expr[Ast.AstNode[_]](Ident(newTermName(cn)))
          val ct: Tree = ValDef(Modifiers(),newTermName(cn), TypeTree(weakTypeOf[Ast.AstNode[_]]), tree)
          Block(v :: ct :: Nil, Apply(Select(Ident(vn), newTermName("consume")), ce.tree :: Nil))
      }
    }

    def buildObject(tpe: Type, reader: c.Expr[Ast.ObjectNode], methodSuffix: String = "", args: List[Tree] = Nil): Tree = {
      if (helper.isCaseClass(tpe)) {
        val TypeRef(_, sym, tpeArgs) = tpe
        val newObjTypeTree = helper.typeArgumentTree(tpe)

        // Builds the if/else tree for checking constructor params and returning a new object
        def pickConstructorTree(argNames: c.Expr[Set[String]]): Tree = {
          // Makes expressions for determining of they list is satisfied by the reader
          def ctorCheckingExpr(ctors: List[List[Symbol]]): c.Expr[Boolean] = {
             def isRequired(item: Symbol) = {
              val sym = item.asTerm
              !(sym.isParamWithDefault || sym.typeSignature <:< typeOf[Option[_]])
            }

            val expr = c.Expr[Set[String]](Apply(Select(Ident(newTermName("Set")), newTermName("apply")),
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

        def buildObjFromParams(ctorParams: List[List[Symbol]]): Tree = {
          val params = ctorParams.map(_.zipWithIndex.map {
            case (pSym, index) =>
              // Change out the types if it has type parameters
              val pTpe = pSym.typeSignature.substituteTypes(sym.asClass.typeParams, tpeArgs)
              val fieldName = Literal(Constant(pSym.name.decoded))
              val pTnm = newTermName(pSym.name.decoded)

              // If param has defaults, try to find the val in map, or call
              // default evaluation from its companion object
              // TODO: is the sym.companionSymbol.isTerm the best way to check for NoSymbol?
              // TODO: is there a way to get teh default values for the overloaded constructors?
              val tree = if (pSym.asTerm.isParamWithDefault && sym.companionSymbol.isTerm) {
                val defVal = Select(Ident(sym.companionSymbol), newTermName("$lessinit$greater$default$" + (index + 1).toString))
                buildValue(pTpe, reader, "Field", List(fieldName), defVal)
              } else {
                buildValue(pTpe, reader, "Field", List(fieldName))
              }
              (ValDef(Modifiers(), pTnm, TypeTree(pTpe), tree), Ident(pTnm))
          })

          Block(params.head.map(_._1), Apply(Select(New(Ident(sym)), nme.CONSTRUCTOR), params.head.map(_._2)))
        }

        val on = c.fresh("consumed$")
        val ot = newTermName(on)
        val otr: Tree = ValDef(Modifiers(), ot, TypeTree(tpe), pickConstructorTree(reify(reader.splice.keySet)))

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

        val setVarsBlocks: List[Tree] = Nil
//          helper.getNonConstructorVars(tpe).toList.map {
//            pSym =>
//              val varName = pSym.name.toTermName.toString.trim
//              val tpe = pSym.typeSignature.substituteTypes(sym.asClass.typeParams, tpeArgs)
//              optionalParams(tpe, varName,
//                tree => c.Expr(Assign(Select(Ident(ot), newTermName(varName)), tree))
//              )
//          }

        val setSetterBlocks: List[Tree] = Nil
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

        Block(otr :: setVarsBlocks ::: setSetterBlocks, Ident(ot))
//        pickConstructorTree(reify(reader.splice.keySet))
      } else {
        c.abort(c.enclosingPosition, "Only case classes with a single constructor are supported")
      }

    }



    reify {
      new Consumer[T] {
//        c.Expr(importExpr).splice

        def consume(node: Ast.AstNode[_]): T = {
//          node match {
//            case obj: Ast.ObjectNode =>
//              val res: T = c.Expr[T](buildObject(thisType, c.Expr[Ast.ObjectNode](Ident(newTermName("obj"))))).splice
//              res
//            case _ => throw new MappingException(s"Got a ${node.getClass.getSimpleName} and expected an ObjectNode")
//          }
          node match {
            case obj: Ast.ObjectNode => c.Expr[T](buildObject(thisType, c.Expr[ObjectNode](Ident(newTermName("obj"))))).splice
//            case arr: ArrayNode => c.Expr[T](buildArray(thisType, c.Expr[ArrayNode](Ident(newTermName("arr"))))).splice
//            case nr: NumberNodeLike[_] => c.Expr[T](buildPrimitive(thisType, c.Expr[ArrayNode](Ident(newTermName("nr"))))).splice
//            case txt: TextNode => c.Expr[T](buildPrimitive(thisType, c.Expr[ArrayNode](Ident(newTermName("txt"))))).splice
//            case bool: BoolNode => c.Expr[T](buildPrimitive(thisType, c.Expr[ArrayNode](Ident(newTermName("bool"))))).splice
            case muster.Ast.NullNode | muster.Ast.UndefinedNode => null.asInstanceOf[T]
            case x => throw new MappingException(s"Got a ${x.getClass.getSimpleName} and expected an ObjectNode")
          }
        }
      }
    }
  }
}