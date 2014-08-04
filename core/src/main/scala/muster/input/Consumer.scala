package muster
package input

import java.{util => jutil}
import java.util.{Locale, Date}

import muster.ast._
import muster.jackson.util.ISO8601Utils
import muster.util.SafeSimpleDateFormat

import scala.annotation.implicitNotFound
import scala.language.experimental.macros
import scala.language.higherKinds
import scala.reflect.macros._
import scala.collection.generic
import scala.reflect.ClassTag
import scala.util.Try

/** Contains the macro implementation for the [[muster.input.Consumer]] type class and a bunch of default implementations
  *
  * @see [[muster.input.Consumer]]
  */
object Consumer {

  type ErrorHandler[S] = PartialFunction[Throwable, S]

  private def cc[S](fn: PartialFunction[AstNode[_], S])(implicit mf: ClassTag[S]): Consumer[S] = new Consumer[S] {
    def consume(node: AstNode[_]): S = if (fn.isDefinedAt(node)) fn(node) else throw new MappingException(s"Couldn't convert $node to ${mf.runtimeClass.getSimpleName}")
  }

  private def nc[S](fn: NumberNodeLike[_] => S)(implicit mf: ClassTag[S]): Consumer[S] = cc[S] {
    case m: NumberNodeLike[_] => fn(m)
    case m: TextNode => fn(NumberNode(m.value))
  }

  /** A Boolean consumer, reads booleans */
  implicit val BooleanConsumer = cc[Boolean]({
    case m: BoolNode => m.value
    case NullNode => false
  })

  /** A Byte consumer, reads bytes */
  implicit val ByteConsumer = nc[Byte](_.toByte)

  /** A Short consumer, reads short numbers */
  implicit val ShortConsumer = nc[Short](_.toShort)

  /** An Int consumer, reads int numbers */
  implicit val IntConsumer = nc[Int](_.toInt)

  /** A Long consumer, reads long numbers */
  implicit val LongConsumer = nc[Long](_.toLong)

  /** A BigInt consumer, reads big integer numbers */
  implicit val BigIntConsumer = nc[BigInt](_.toBigInt)

  /** A Float consumer, reads floating point numbers */
  implicit val FloatConsumer = nc[Float](_.toFloat)

  /** A Double consumer, reads double numbers */
  implicit val DoubleConsumer = nc[Double](_.toDouble)

  /** A BigDecimal consumer, reads big decimal numbers */
  implicit val BigDecimalConsumer = nc[BigDecimal](_.toBigDecimal)

  /** A Byte consumer, reads bytes */
  implicit val JavaByteConsumer = nc[java.lang.Byte](v => byte2Byte(v.toByte))

  /** A Short consumer, reads short numbers */
  implicit val JavaShortConsumer = nc[java.lang.Short](v => short2Short(v.toShort))

  /** An Int consumer, reads int numbers */
  implicit val JavaIntConsumer = nc[java.lang.Integer](v => int2Integer(v.toInt))

  /** A Long consumer, reads long numbers */
  implicit val JavaLongConsumer = nc[java.lang.Long](v => long2Long(v.toLong))

  /** A BigInt consumer, reads big integer numbers */
  implicit val JavaBigIntConsumer = nc[java.math.BigInteger](_.toBigInt.bigInteger)

  /** A Float consumer, reads floating point numbers */
  implicit val JavaFloatConsumer = nc[java.lang.Float](v => float2Float(v.toFloat))

  /** A Double consumer, reads double numbers */
  implicit val JavaDoubleConsumer = nc[java.lang.Double](v => double2Double(v.toDouble))

  /** A BigDecimal consumer, reads big decimal numbers */
  implicit val JavaBigDecimalConsumer = nc[java.math.BigDecimal](_.toBigDecimal.bigDecimal)

  /** A String consumer, reads strings */
  implicit val StringConsumer = cc[String] {
    case TextNode(value) => value
    case NumberNode(value) => value
    case m: NumberNodeLike[_] => m.value.toString
    case NullNode => null
  }

  /** A Symbol consumer, reads scala symbols */
  implicit val SymbolConsumer = new Consumer[scala.Symbol] {
    def consume(node: AstNode[_]): Symbol = Symbol(StringConsumer.consume(node))
  }

  /** A Date consumer, reads ISO8601 dates that follow this pattern yyyy-MM-ddThh:mm:ss[.sss][Z|[+-]hh:mm] */
  implicit val Iso8601DateConsumer = cc[Date]({
    case TextNode(value) => ISO8601Utils.parse(value)
    case m: NumberNodeLike[_] => new Date(m.toLong)
    case NullNode => null.asInstanceOf[Date]
  })

  /** Creates a date consumer for the specified pattern, this consumer is thread-safe
    *
    * @see http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
    * @param pattern the simple date format pattern
    * @param locale the locale to use for this date consumer
    * @return a [[muster.input.Consumer]] that produces a [[java.util.Date]]
    */
  def dateConsumer(pattern: String, locale: Locale = SafeSimpleDateFormat.DefaultLocale) = {
    cc[Date]({
      case TextNode(value) => new SafeSimpleDateFormat(pattern, locale).parse(value)
      case m: NumberNodeLike[_] => new Date(m.toLong)
      case NullNode => null
    })
  }

  /** A map consumer, reads scala map objects
    *
    * @param keySerializer the [[muster.MapKeySerializer]] to use for serializing keys of type [[K]] to a string
    * @param valueConsumer the [[muster.input.Consumer]] to use for reading the values into the map
    * @tparam F the type of map to build
    * @tparam K the type for the key of the [[F]]
    * @tparam V the type for the value of the [[F]]
    * @return a [[muster.input.Consumer]] that consumes [[F]] for types [[K]] and [[V]]
    */
  implicit def mapConsumer[F[_, _], K, V](implicit cbf: generic.CanBuildFrom[F[_, _], (K, V), F[K, V]], keySerializer: MapKeySerializer[K], valueConsumer: Consumer[V]) = new Consumer[F[K, V]] {
    def consume(node: AstNode[_]) = node match {
      case m: ObjectNode =>
        val bldr = cbf()
        m.keySet foreach { key =>
          bldr += keySerializer.deserialize(key) -> valueConsumer.consume(m.readField(key))
        }
        bldr.result()
      case NullNode => null.asInstanceOf[F[K, V]]
      case x => throw new MappingException(s"Got a ${x.getClass.getSimpleName} and expected an ObjectNode")
    }
  }

  /** A traversable consumer, reads scala collections
    *
    * @param cbf the [[scala.collection.generic.CanBuildFrom]] for the traversable to build
    * @param valueReader the [[muster.input.Consumer]] to read the values for this traversable
    * @tparam F the type of collection to build
    * @tparam V the value type for the collection elements
    * @return a [[muster.input.Consumer]] for the traversable defined by [[F]]
    */
  implicit def traversableConsumer[F[_], V](implicit cbf: generic.CanBuildFrom[F[_], V, F[V]], valueReader: Consumer[V]): Consumer[F[V]] =
    new Consumer[F[V]] {
      def consume(node: AstNode[_]): F[V] = node match {
        case m: ArrayNode =>
          val bldr = cbf()
          while (m.hasNextNode) {
            bldr += valueReader.consume(m.nextNode())
          }
          bldr.result()
        case NullNode => null.asInstanceOf[F[V]]
        case x => throw new MappingException(s"Got a ${x.getClass.getSimpleName} and expected an ArrayNode")
      }
    }

  /** A java list consumer, reads [[java.util.List]] objects
    *
    * @param valueConsumer the consumer for value type [[T]]
    * @tparam T the value type of the [[java.util.List]]
    * @return a consumer for [[java.util.List]] of type [[T]]
    */
  implicit def javaListConsumer[T](implicit valueConsumer: Consumer[T]) = cc[java.util.List[T]] {
    case m: ArrayNode =>
      val lst = new java.util.ArrayList[T]()
      while (m.hasNextNode) {
        lst add valueConsumer.consume(m.nextNode())
      }
      lst
    case NullNode => null.asInstanceOf[java.util.List[T]]
  }


  /** A java set consumer, reads [[java.util.Set]] objects
    *
    * @param valueConsumer the consumer for value type [[T]]
    * @tparam T the value type of the [[java.util.Set]]
    * @return a consumer for [[java.util.Set]] of type [[T]]
    */
  implicit def javaSetConsumer[T](implicit valueConsumer: Consumer[T]) = cc[java.util.Set[T]] {
    case m: ArrayNode =>
      val lst = new java.util.HashSet[T]()
      while (m.hasNextNode) {
        lst add valueConsumer.consume(m.nextNode())
      }
      lst
    case NullNode => null.asInstanceOf[java.util.Set[T]]
  }


  /** A java map consumer, reads [[java.util.Map]] objects
    *
    * @param valueConsumer the consumer for value type [[T]]
    * @tparam K the key type of the [[java.util.Map]]
    * @tparam T the value type of the [[java.util.Map]]
    * @return a consumer for [[java.util.Map]] of types [[K]] and [[T]]
    */
  implicit def javaMapConsumer[K, T](implicit keySerializer: MapKeySerializer[K], valueConsumer: Consumer[T]) = cc[java.util.Map[K, T]] {
    case m: ObjectNode =>
      val lst = new jutil.HashMap[K, T]()
      m.keySet foreach { key =>
        lst.put(keySerializer.deserialize(key), valueConsumer.consume(m.readField(key)))
      }
      lst
    case NullNode  => null.asInstanceOf[java.util.Map[K, T]]
  }

  /** An array consumer, reads an array
    *
    * @param ct the class tag of the element type
    * @param valueReader the [[muster.input.Consumer]] for the element type
    * @tparam T the element type
    * @return a [[muster.input.Consumer]] for a [[scala.Array]] of type [[T]]
    */
  implicit def arrayConsumer[T](implicit ct: ClassTag[T], valueReader: Consumer[T]): Consumer[Array[T]] = cc[Array[T]] {
    case m: ArrayNode =>
      val bldr = Array.newBuilder
      while (m.hasNextNode) {
        bldr += valueReader.consume(m.nextNode())
      }
      bldr.result()
    case NullNode => null.asInstanceOf[Array[T]]
  }

  /** An option consumer, reads a [[scala.Option]]
    *
    * @param valueReader the reader for the type [[T]]
    * @tparam T the value type of the option
    * @return a [[muster.input.Consumer]] for a [[scala.Option]] of type [[T]]
    */
  implicit def optionConsumer[T](implicit valueReader: Consumer[T]): Consumer[Option[T]] = new Consumer[Option[T]] {

    /** Converts a [[AstNode]] into a [[scala.Option]] of type [[T]]
      *
      * @param node the [[AstNode]] to convert
      * @return a [[scala.Option]] for type [[T]]
      * @throws a [[muster.MappingException]] when the node couldn't be converted into [[scala.Option]] of [[T]]
      * @throws a [[muster.ParseException]] when the source stream contains invalid characters
      */
    def consume(node: AstNode[_]): _root_.scala.Option[T] = node match {
      case NullNode | UndefinedNode => None
      case v => Try(valueReader.consume(v)).map(Option(_)).recover(recover).get
    }

    /** Provides a recovery mechanism for certain exceptions
      *
      * This is useful for options and disjunctions
      * @return the recovered value if there is one for the provided exception
      */
    override def recover: ErrorHandler[Option[T]] = {
      case _: EndOfInput => None
    }
  }

  /** Provides a reader that catches throwables and turns them into the left side of an either.
    *
    * This consumer also takes the recover handler on the valueReader into account
    *
    * @param valueReader the reader for the right side of the either
    * @tparam R The value type for the right side of the either
    * @return a [[muster.input.Consumer]] for a [[scala.Either]]
    */
  implicit def throwableEitherConsumer[R](implicit valueReader: Consumer[R]) = new Consumer[Either[Throwable, R]] {
     def consume(node: AstNode[_]): scala.Either[scala.Throwable, R] = {
       (Try(Right(valueReader.consume(node))) recover {
         case t if valueReader.recover.isDefinedAt(t) => Right(valueReader.recover(t))
         case t => Left(t)
       }).get
     }
  }


  /** Provides a reader for a scala either
    *
    * This consumer first tries to deserialize the right side if that fails, it tries the right side recove handler.
    * If both of those things fail it will try and parse the node as a left side.
    *
    * @param leftReader the reader to use for the left side of the either
    * @param rightReader the reader to use for the right side of the either
    * @tparam L the type for the left side of the either
    * @tparam R the type for the right side of the either
    * @return a [[muster.input.Consumer]] for a [[scala.Either]] of types [[L]] and [[R]]
    */
  implicit def eitherConsumer[L, R](implicit leftReader: Consumer[L], rightReader: Consumer[R]): Consumer[Either[L, R]] = new Consumer[Either[L,R]] {
     def consume(node: AstNode[_]): scala.Either[L, R] =
      (Try(Right(rightReader.consume(node))) recover {
        case t if rightReader.recover.isDefinedAt(t) => Right(rightReader.recover(t))
        case _ => Left(leftReader.consume(node))
      }).get
  }

  /** The macro implementation of the consumer type class
    *
    * This macro builds deserializers for instances of type [[T]].
    * The deserializer makes use of a [[AstNode]] to possibly walk the values for the properties of [[T]]
    * @tparam T the type to build instances for
    * @return a [[muster.input.Consumer]] for instance of type [[T]]
    */
  implicit def consumer[T]: Consumer[T] = macro consumerImpl[T]

  def consumerImpl[T: c.WeakTypeTag](c: Context): c.Expr[Consumer[T]] = {
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

    val nullNodeDefault = reify(NullNode).tree

    def buildValue(_tpe: Type, reader: c.Expr[Any], methodSuffix: String = "", args: List[Tree] = Nil, default: Tree = nullNodeDefault): (Tree, Tree) = {
      // TODO: put in separate file for 2.10, 2.11 support, was _tpe.dealias
      val tpe = _tpe.normalize
      val t = appliedType(weakTypeOf[Consumer[Any]].typeConstructor, tpe :: Nil)
      c.inferImplicitValue(t) match {
        case EmptyTree => c.abort(c.enclosingPosition, s"Couldn't find a muster.input.Consumer[${t.typeSymbol.name.decodedName.toString}], try bringing an implicit value for ${tpe.typeSymbol.name.decodedName.toString} in scope by importing one or defining one.")
        case resolved =>

          val rdrOpt: Tree = {
            if (helper.isPrimitive(tpe)) {
              val nm = primitiveMap(tpe)
              Apply(Select(reader.tree, newTermName(s"read${nm}${methodSuffix}Opt")), args)
            } else if (helper.isOption(tpe)) {
              val agType = helper.resolveInnerOptionType(tpe)
              val nm = primitiveMap(agType)
              Apply(Select(reader.tree, newTermName(s"read${nm}${methodSuffix}Opt")), args)
            } else if (helper.isSeq(tpe) || helper.isSet(tpe))
              Apply(Select(reader.tree, newTermName(s"readArray${methodSuffix}Opt")), args)
            else if (helper.isEnum(tpe)) {
              Apply(Select(reader.tree, newTermName(s"readString${methodSuffix}Opt")), args)
            } else
              Apply(Select(reader.tree, newTermName(s"readObject${methodSuffix}Opt")), args)
          }

          (rdrOpt, resolved)
      }
    }

    def setterDef(returnType: Type, reader: c.Expr[Any], fieldName: Tree, defVal: Tree = EmptyTree): Tree = {
      val t = appliedType(weakTypeOf[Consumer[Any]].typeConstructor, returnType :: Nil)
      val (definition, resolved) = buildValue(returnType, reader, "Field", List(fieldName), defVal)
      val fn = c.fresh("consumer$")
      val vn = newTermName(fn)
      val v = ValDef(Modifiers(), vn, TypeTree(t), resolved)
      val cn = c.fresh("node$")

      defVal match {
        case EmptyTree =>
          val noDefault = Apply(Select(definition, newTermName("getOrElse")), nullNodeDefault :: Nil)
          val ce = c.Expr[AstNode[_]](Ident(newTermName(cn)))
          val ct: Tree = ValDef(Modifiers(), newTermName(cn), TypeTree(weakTypeOf[AstNode[_]]), noDefault)
          Block(v :: ct :: Nil, Apply(Select(Ident(vn), newTermName("consume")), ce.tree :: Nil))
        case defTree =>
          val ce = c.Expr[Option[AstNode[_]]](Ident(newTermName(cn)))
          val ct: Tree = ValDef(Modifiers(), newTermName(cn), TypeTree(weakTypeOf[Option[AstNode[_]]]), definition)
          //          val cons = Apply(Select(Ident(vn), TermName("consume")), ce.tree :: Nil)
          val res = reify(
            ce.splice match {
              case Some(n) => c.Expr(Apply(Select(Ident(vn), newTermName("consume")), Ident(newTermName("n")) :: Nil)).splice
              case _ => c.Expr(defTree).splice
            })
          Block(v :: ct :: Nil, res.tree)
      }


    }

    def buildObject(tpe: Type, reader: c.Expr[ObjectNode], methodSuffix: String = "", args: List[Tree] = Nil): Tree = {
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

            val expr = c.Expr[Set[String]](Apply(Select(Ident(newTermName("Set")), newTermName("apply")),
              ctors.flatten.filter(isRequired).map(sym => Literal(Constant(sym.name.decodedName.toString)))
            ))

            reify(expr.splice.subsetOf(argNames.splice))
          }

          def ifElseTreeBuilder(ctorSets: List[(c.Expr[Boolean], List[List[Symbol]])]): Tree = ctorSets match {
            case Nil => EmptyTree
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
              val fieldName = Literal(Constant(pSym.name.decodedName.toString))
              val pTnm = newTermName(pSym.name.decodedName.toString)

              // If param has defaults, try to find the val in map, or call
              // default evaluation from its companion object
              // is the sym.companionSymbol.isTerm the best way to check for NoSymbol?
              // is there a way to get teh default values for the overloaded constructors?
              val tree = if (pSym.asTerm.isParamWithDefault && sym.companionSymbol.isTerm) {
                val defVal = Select(Ident(sym.companionSymbol), newTermName("$lessinit$greater$default$" + (index + 1).toString))
                //                val defValG = Apply(Select(New(weakTypeOf[ConstantNode[_]])))
                setterDef(pTpe, reader, fieldName, defVal)
              } else {
                setterDef(pTpe, reader, fieldName)
              }
              (ValDef(Modifiers(), pTnm, TypeTree(pTpe), tree), Ident(pTnm))
          })

          Block(params.flatMap(_.map(_._1)), params.foldLeft(Select(New(Ident(sym)), nme.CONSTRUCTOR): Tree) { (ct, args) =>
            Apply(ct, args.map(_._2))
          })
        }

        val on = c.fresh("consumed$")
        val ot = newTermName(on)
        val otr: Tree = ValDef(Modifiers(), ot, TypeTree(tpe), pickConstructorTree(reify(reader.splice.keySet)))

        val setterBlocks: List[Tree] = {
          helper.getSetters(tpe) map { pSym =>
            val needsLower = pSym.name.decodedName.toString.startsWith("set")
            val stripped = pSym.name.decodedName.toString.replaceFirst("^set", "").replaceFirst("_=$", "")
            val name = if (needsLower) stripped(0).toLower + stripped.substring(1) else stripped
            val paramType = {
              val tp = pSym.asMethod.paramss.head.head
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
        def consume(node: AstNode[_]): T = {
          node match {
            case obj: ObjectNode => c.Expr[T](buildObject(thisType, c.Expr[ObjectNode](Ident(newTermName("obj"))))).splice
            case NullNode | UndefinedNode => null.asInstanceOf[T]
            case x => throw new MappingException(s"Got a ${x.getClass.getSimpleName} and expected an ObjectNode")
          }
        }
      }
    }
  }
}

/** The type class that allows for reading streams and building objects from those streams.
  *
  * It receives an AST node and is expected to return the resulting target type from that node.
  * Typically this happens with some pattern matches.
  *
  * @example A string consumer
  *          {{{
  *           implicit object StringConsumer extends Consumer[String] {
  *             def consume(node: AstNode[_]): String = node match {
  *               case TextNode(value) => value
  *               case NumberNode(value) => value
  *               case m: NumberNodeLike[_] => m.value.toString
  *               case NullNode => null
  *               case _ => throw new MappingException(s"Couldn't convert $node to String")
  *             }
  *           }
  *          }}}
  *
  * @example A custom serializer for a person
  *          {{{
  *          case class Person(id: Long, name: String, age: Int)
  *
  *          implicit object PersonConsumer extends Consumer[Person] {
  *            def consume(node: AstNode[_]): Person = node match {
  *              case obj: ObjectNode =>
  *                val addressesConsumer = implicitly[Consumer[Seq[Address]]]
  *                Person(
  *                  obj.readIntField("id"),
  *                  obj.readStringField("name"),
  *                  addressesConsumer.consume(obj.readArrayField("addresses"))
  *                )
  *              case _ => throw new MappingException(s"Can't convert a ${node.getClass} to a Person")
  *            }
  *          }
  *          }}}
  *
  * @tparam S The type of object this consumer builds
  */
@implicitNotFound("Couldn't find a Consumer for ${S}. Try importing muster._ or to implement a muster.input.Consumer")
trait Consumer[S] {

  /** Converts a [[AstNode]] into the type [[S]]
    *
    * @param node the [[AstNode]] to convert
    * @return a [[S]]
    * @throws a [[muster.MappingException]] when the node couldn't be converted into [[S]]
    * @throws a [[muster.ParseException]] when the source stream contains invalid characters
    */
  def consume(node: AstNode[_]): S

  /** Provides a recovery mechanism for certain exceptions
    *
    * This is useful for options and disjunctions
    * @return the recovered value if there is one for the provided exception
    */
  def recover: Consumer.ErrorHandler[S] = PartialFunction.empty[Throwable, S]
}