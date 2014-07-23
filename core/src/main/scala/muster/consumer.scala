package muster

import scala.language.experimental.macros
import scala.reflect.macros._
import scala.reflect.ClassTag
import java.util.Date
import scala.collection.{generic, immutable}
import scala.util.Try
import java.util
import Ast._

//import org.joda.time.DateTime

trait Consumer[S] {
  def consume(node: AstNode[_]): S
}

object Consumer {

  def nullValue = Ast.NullNode

  private def cc[S](fn: PartialFunction[AstNode[_], S])(implicit mf: ClassTag[S]) = new Consumer[S] {
    def consume(node: AstNode[_]): S = if (fn.isDefinedAt(node)) fn(node) else throw new MappingException(s"Couldn't convert $node to ${mf.runtimeClass.getSimpleName}")
  }

  private def nc[S](fn: NumberNodeLike[_] => S)(implicit mf: ClassTag[S]) = cc[S] {
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
    case TextNode(value) => SafeSimpleDateFormat.Iso8601Formatter.parse(value)
    case m: NumberNodeLike[_] => new Date(m.toLong)
    case NullNode | UndefinedNode => null
  })
//
//  implicit val Iso8601DateTimeConsumer = cc[DateTime]({
//    case TextNode(value) => new DateTime(SafeSimpleDateFormat.Iso8601Formatter.parse(value))
//    case m: NumberNodeLike[_] => new DateTime(m.toLong)
//    case NullNode | UndefinedNode => null
//  })

  def dateConsumer(pattern: String) = {
    cc[Date]({
      case TextNode(value) => new SafeSimpleDateFormat(pattern).parse(value)
      case m: NumberNodeLike[_] => new Date(m.toLong)
      case NullNode | UndefinedNode => null
    })
  }


  implicit def mapConsumer[K, V](implicit keySerializer: MapKeySerializer[K], valueConsumer: Consumer[V]) = cc[immutable.Map[K, V]] {
    case m: ObjectNode =>
      val bldr = Map.newBuilder[K, V]
      m.keySet foreach { key =>
        bldr += keySerializer.deserialize(key) -> valueConsumer.consume(m.readField(key))
      }
      bldr.result()
    case NullNode | UndefinedNode => null
  }


  implicit def mutableMapConsumer[K, V](implicit keySerializer: MapKeySerializer[K], valueConsumer: Consumer[V]) = cc[scala.collection.mutable.Map[K, V]] {
    case m: ObjectNode =>
      val bldr = collection.mutable.Map.newBuilder[K, V]
      m.keySet foreach { key =>
        bldr += keySerializer.deserialize(key) -> valueConsumer.consume(m.readField(key))
      }
      bldr.result()
    case NullNode | UndefinedNode => null
  }

  import scala.language.higherKinds

  implicit def traversableConsumer[F[_], V](implicit cbf: generic.CanBuildFrom[F[_], V, F[V]], valueReader: Consumer[V]): Consumer[F[V]] =
    new Consumer[F[V]] {
      def consume(node: AstNode[_]): F[V] = node match {
        case m: ArrayNode =>
          val bldr = cbf()
          while (m.hasNextNode) {
            bldr += valueReader.consume(m.nextNode())
          }
          bldr.result()
        case NullNode | UndefinedNode => cbf().result()
        case x => throw new MappingException(s"Got a ${x.getClass.getSimpleName} and expected an ArrayNode")
      }
    }

  implicit def javaListConsumer[T](implicit valueConsumer: Consumer[T]) = cc[java.util.List[T]] {
    case m: ArrayNode =>
      val lst = new java.util.ArrayList[T]()
      while (m.hasNextNode) {
        lst add valueConsumer.consume(m.nextNode())
      }
      lst
    case NullNode | UndefinedNode => new util.ArrayList[T]()
  }

  implicit def javaSetConsumer[T](implicit valueConsumer: Consumer[T]) = cc[java.util.Set[T]] {
    case m: ArrayNode =>
      val lst = new java.util.HashSet[T]()
      while (m.hasNextNode) {
        lst add valueConsumer.consume(m.nextNode())
      }
      lst
    case NullNode | UndefinedNode => new util.HashSet[T]()
  }

  implicit def javaMapConsumer[K, T](implicit keySerializer: MapKeySerializer[K], valueConsumer: Consumer[T]) = cc[java.util.Map[K, T]] {
    case m: ObjectNode =>
      val lst = new util.HashMap[K, T]()
      m.keySet foreach { key =>
        lst.put(keySerializer.deserialize(key), valueConsumer.consume(m.readField(key)))
      }
      lst
    case NullNode | UndefinedNode => new util.HashMap[K, T]()
  }

  implicit def arrayConsumer[T](implicit ct: ClassTag[T], valueReader: Consumer[T]): Consumer[Array[T]] = cc[Array[T]] {
    case m: ArrayNode =>
      val bldr = Array.newBuilder
      while (m.hasNextNode) {
        bldr += valueReader.consume(m.nextNode())
      }
      bldr.result()
    case NullNode | UndefinedNode => Array.newBuilder[T].result()
  }

  implicit def optionConsumer[T](implicit valueReader: Consumer[T]): Consumer[Option[T]] = cc[Option[T]] {
    case NullNode | UndefinedNode => None
    case v => Try(valueReader.consume(v)).toOption
  }

  implicit def consumer[T]: Consumer[T] = macro consumerImpl[T]

  def consumerImpl[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[Consumer[T]] = {
    import c.universe._

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
      ).withDefault(v => v.typeSymbol.name.decodedName.toString)


    val helper = new Helper[c.type](c)
    val thisType = weakTypeOf[T]

//    val importExpr = c.parse(s"import ${thisType.normalize.typeConstructor.typeSymbol.fullName}")

    //    def buildPrimitive(tpe: Type, cursor: c.Expr[Any], methodNameSuffix: String = "", args: List[Tree] = Nil): Tree = EmptyTree
    //    def buildArray(tpe: Type, cursor: c.Expr[Any], methodNameSuffix: String = "", args: List[Tree] = Nil): Tree = EmptyTree

    val nullNodeDefault = reify(Ast.NullNode).tree

    def buildValue(_tpe: Type, reader: c.Expr[Any], methodSuffix: String = "", args: List[Tree] = Nil, default: Tree = nullNodeDefault): (Tree, Tree) = {
      val tpe = _tpe.dealias
      val t = appliedType(weakTypeOf[Consumer[Any]].typeConstructor, tpe :: Nil)
      c.inferImplicitValue(t) match {
        case EmptyTree => c.abort(c.enclosingPosition, s"Couldn't find a muster.Consumer[${t.typeSymbol.name.decodedName.toString}], try bringing an implicit value for ${tpe.typeSymbol.name.decodedName.toString} in scope by importing one or defining one.")
        case resolved =>

          val rdrOpt: Tree = {
            if (helper.isPrimitive(tpe)) {
              val nm = primitiveMap(tpe)
              Apply(Select(reader.tree, TermName(s"read${nm}${methodSuffix}Opt")), args)
            } else if (helper.isOption(tpe)) {
              val agType = helper.resolveInnerOptionType(tpe)
              val nm = primitiveMap(agType)
              Apply(Select(reader.tree, TermName(s"read${nm}${methodSuffix}Opt")), args)
            } else if (helper.isSeq(tpe) || helper.isSet(tpe))
              Apply(Select(reader.tree, TermName(s"readArray${methodSuffix}Opt")), args)
            else if (helper.isEnum(tpe)) {
              Apply(Select(reader.tree, TermName(s"readString${methodSuffix}Opt")), args)
            } else
              Apply(Select(reader.tree, TermName(s"readObject${methodSuffix}Opt")), args)
          }

          (rdrOpt, resolved)
        //          (default match {
        //            case EmptyTree =>
        //              rdrOpt
        //            case x =>
        //              Apply(Select(rdrOpt, TermName("getOrElse")), default :: Nil)
        //          }, resolved)
      }
    }

    def setterDef(returnType: Type, reader: c.Expr[Any], fieldName: Tree, defVal: Tree = EmptyTree): Tree = {
      val t = appliedType(weakTypeOf[Consumer[Any]].typeConstructor, returnType :: Nil)
      val (definition, resolved) = buildValue(returnType, reader, "Field", List(fieldName), defVal)
      val fn = c.freshName("consumer$")
      val vn = TermName(fn)
      val v = ValDef(Modifiers(), vn, TypeTree(t), resolved)
      val cn = c.freshName("node$")

      defVal match {
        case EmptyTree =>
          val noDefault = Apply(Select(definition, TermName("getOrElse")), nullNodeDefault :: Nil)
          val ce = c.Expr[Ast.AstNode[_]](Ident(TermName(cn)))
          val ct: Tree = ValDef(Modifiers(), TermName(cn), TypeTree(weakTypeOf[Ast.AstNode[_]]), noDefault)
          Block(v :: ct :: Nil, Apply(Select(Ident(vn), TermName("consume")), ce.tree :: Nil))
        case defTree =>
          val ce = c.Expr[Option[Ast.AstNode[_]]](Ident(TermName(cn)))
          val ct: Tree = ValDef(Modifiers(), TermName(cn), TypeTree(weakTypeOf[Option[AstNode[_]]]), definition)
          //          val cons = Apply(Select(Ident(vn), TermName("consume")), ce.tree :: Nil)
          val res = reify(
            ce.splice match {
              case Some(n) => c.Expr(Apply(Select(Ident(vn), TermName("consume")), Ident(TermName("n")) :: Nil)).splice
              case _ => c.Expr(defTree).splice
            })
          Block(v :: ct :: Nil, res.tree)
      }


    }

    def buildObject(tpe: Type, reader: c.Expr[Ast.ObjectNode], methodSuffix: String = "", args: List[Tree] = Nil): Tree = {
      if (tpe.typeSymbol.isClass && !(helper.isPrimitive(tpe) || helper.isMap(tpe) || helper.isOption(tpe) || helper.isSeq(tpe) || helper.isEither(tpe) || helper.isEnum(tpe))) {
        val TypeRef(_, sym, tpeArgs) = tpe

        // Builds the if/else tree for checking constructor params and returning a new object
        def pickConstructorTree(argNames: c.Expr[Set[String]]): Tree = {
          // Makes expressions for determining of they list is satisfied by the reader
          def ctorCheckingExpr(ctors: List[List[Symbol]]): c.Expr[Boolean] = {
            def isRequired(item: Symbol) = {
              val sym = item.asTerm
              !(sym.isParamWithDefault || sym.typeSignature <:< typeOf[Option[_]])
            }

            val expr = c.Expr[Set[String]](Apply(Select(Ident(TermName("Set")), TermName("apply")),
              ctors.flatten.filter(isRequired).map(sym => Literal(Constant(sym.name.decodedName.toString)))
            ))

            reify(expr.splice.subsetOf(argNames.splice))
          }

          def ifElseTreeBuilder(ctorSets: List[(c.Expr[Boolean], List[List[Symbol]])]): Tree = ctorSets match {
            case Nil => EmptyTree
            case h :: Nil => buildObjFromParams(h._2)
            case h :: t => If(h._1.tree, buildObjFromParams(h._2), ifElseTreeBuilder(t))
          }



          val ctors: List[MethodSymbol] = tpe.member(termNames.CONSTRUCTOR)
            .asTerm.alternatives // List of constructors
            .map(_.asMethod) // method symbols
            .sortBy(-_.paramLists.flatten.size)
          val ifExprsAndParams = ctors.map(ctor => ctorCheckingExpr(ctor.paramLists)).zip(ctors.map(_.asMethod.paramLists))

          ifElseTreeBuilder(ifExprsAndParams)
        }



        def buildObjFromParams(ctorParams: List[List[Symbol]]): Tree = {
          val params = ctorParams.map(_.zipWithIndex.map {
            case (pSym, index) =>
              // Change out the types if it has type parameters
              val pTpe = pSym.typeSignature.substituteTypes(sym.asClass.typeParams, tpeArgs)
              val fieldName = Literal(Constant(pSym.name.decodedName.toString))
              val pTnm = TermName(pSym.name.decodedName.toString)

              // If param has defaults, try to find the val in map, or call
              // default evaluation from its companion object
              // TODO: is the sym.companionSymbol.isTerm the best way to check for NoSymbol?
              // TODO: is there a way to get teh default values for the overloaded constructors?
              val tree = if (pSym.asTerm.isParamWithDefault && sym.companion.isTerm) {
                val defVal = Select(Ident(sym.companion), TermName("$lessinit$greater$default$" + (index + 1).toString))
                //                val defValG = Apply(Select(New(weakTypeOf[ConstantNode[_]])))
                setterDef(pTpe, reader, fieldName, defVal)
              } else {
                setterDef(pTpe, reader, fieldName)
              }
              (ValDef(Modifiers(), pTnm, TypeTree(pTpe), tree), Ident(pTnm))
          })

          Block(params.flatMap(_.map(_._1)), params.foldLeft(Select(New(Ident(sym)), termNames.CONSTRUCTOR): Tree) { (ct, args) =>
            Apply(ct, args.map(_._2))
          })
        }

        val on = c.freshName("consumed$")
        val ot = TermName(on)
        val otr: Tree = ValDef(Modifiers(), ot, TypeTree(tpe), pickConstructorTree(reify(reader.splice.keySet)))

        val setterBlocks: List[Tree] = {
          helper.getSetters(tpe) map { pSym =>
            val needsLower = pSym.name.decodedName.toString.startsWith("set")
            val stripped = pSym.name.decodedName.toString.replaceFirst("^set", "").replaceFirst("_=$", "")
            val name = if (needsLower) stripped(0).toLower + stripped.substring(1) else stripped
            val paramType = {
              val tp = pSym.asMethod.paramLists.head.head
              tp.typeSignatureIn(tpe)
            }
            Apply(Select(Ident(ot), pSym.name), setterDef(paramType, reader, Literal(Constant(name))) :: Nil)
          }
        }

        Block(otr :: setterBlocks, Ident(ot))
      } else {
        c.abort(c.enclosingPosition, "Lists, Maps, Options and Primitives don't use macros")
      }

    }



    reify {
      new Consumer[T] {
        def consume(node: Ast.AstNode[_]): T = {
          node match {
            case obj: Ast.ObjectNode => c.Expr[T](buildObject(thisType, c.Expr[ObjectNode](Ident(TermName("obj"))))).splice
            case muster.Ast.NullNode | muster.Ast.UndefinedNode => null.asInstanceOf[T]
            case x => throw new MappingException(s"Got a ${x.getClass.getSimpleName} and expected an ObjectNode")
          }
        }
      }
    }
  }
}