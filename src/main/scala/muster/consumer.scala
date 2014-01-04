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

trait Consumer[S] {
  def consume(node: AstNode[_]): S
}

object Consumer {

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
  implicit val StringConsumer = cc[String] {
    case TextNode(value) => value
    case NumberNode(value) => value
    case m: NumberNodeLike[_] => m.value.toString
    case NullNode | UndefinedNode => null
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

//  implicit def consumer[T]: Consumer[T] = macro consumerImpl[T]

  def consumerImpl[T: c.WeakTypeTag](c: Context): c.Expr[Consumer[T]] = {
    import c.universe._
    import definitions._
    import Flag._
    val helper = new Helper[c.type](c)
    val thisType = weakTypeOf[T]

    val importExpr = c.parse(s"import ${thisType.normalize.typeConstructor.typeSymbol.fullName}")

    def buildPrimitive(tpe: Type, cursor: c.Expr[Any], methodNameSuffix: String = "", args: List[Tree] = Nil): Tree = EmptyTree
    def buildObject(tpe: Type, cursor: c.Expr[Any], methodNameSuffix: String = "", args: List[Tree] = Nil): Tree = EmptyTree
    def buildArray(tpe: Type, cursor: c.Expr[Any], methodNameSuffix: String = "", args: List[Tree] = Nil): Tree = EmptyTree


    reify {
      new Consumer[T] {
        c.Expr(importExpr).splice

        def consume(node: Ast.AstNode[_]): T = {
          node match {
            case obj: ObjectNode => c.Expr[T](buildObject(thisType, c.Expr[ObjectNode](Ident(newTermName("obj"))))).splice
            case arr: ArrayNode => c.Expr[T](buildArray(thisType, c.Expr[ArrayNode](Ident(newTermName("arr"))))).splice
            case nr: NumberNodeLike[_] => c.Expr[T](buildPrimitive(thisType, c.Expr[ArrayNode](Ident(newTermName("nr"))))).splice
            case txt: TextNode => c.Expr[T](buildPrimitive(thisType, c.Expr[ArrayNode](Ident(newTermName("txt"))))).splice
            case bool: BoolNode => c.Expr[T](buildPrimitive(thisType, c.Expr[ArrayNode](Ident(newTermName("bool"))))).splice
            case NullNode | UndefinedNode => null.asInstanceOf[T]
          }
        }
      }
    }
  }
}