package muster

import scala.language.experimental.macros
import scala.reflect.macros._

object TypeHint {

  def short[T, C <: T](fieldName: String = "$jsonType"): TypeHint[T, C] = macro shortImpl[T, C]
  def full[T, C <: T](fieldName: String = "$jsonType"): TypeHint[T, C] = macro fullImpl[T, C]

  def shortImpl[T: c.WeakTypeTag, C <: T : c.WeakTypeTag](c: blackbox.Context)(fieldName: c.Expr[String]): c.Expr[TypeHint[T, C]] = {
    val tpe = c.weakTypeOf[C].dealias
    generate[T, C](c)(fieldName, tpe.typeSymbol.name.decodedName.toString, tpe)
  }
  def fullImpl[T: c.WeakTypeTag, C <: T : c.WeakTypeTag](c: blackbox.Context)(fieldName: c.Expr[String]): c.Expr[TypeHint[T, C]] = {
    val tpe = c.weakTypeOf[C].dealias
    generate[T, C](c)(fieldName, tpe.typeSymbol.fullName, tpe)
  }

  private def generate[T:c.WeakTypeTag, C <: T : c.WeakTypeTag](c: blackbox.Context)(field: c.Expr[String], nameValue: String, tpe: c.Type): c.Expr[TypeHint[T, C]] = {
    import c.universe._
    val fn = tpe.typeSymbol.fullName
    val ct = appliedType(typeOf[Consumer[Any]], tpe :: Nil)
    val pt = appliedType(typeOf[Producer[Any]], tpe :: Nil)
    val ce =  c.inferImplicitValue(ct) match {
      case EmptyTree =>
        c.abort(c.enclosingPosition, s"Couldn't find a muster.Consumer[$fn], try bringing an implicit value for ${ct.typeSymbol.fullName} in scope by importing one or defining one.")
      case resolved => c.Expr[Consumer[C]](resolved)
    }
    val pe = c.inferImplicitValue(pt) match {
      case EmptyTree =>
        c.abort(c.enclosingPosition, s"Couldn't find a muster.Producer[$fn], try bringing an implicit value for ${pt.typeSymbol.fullName} in scope by importing one or defining one.")
      case resolved => c.Expr[Producer[C]](resolved)
    }


    reify {
      new TypeHint[T, C] {
        val fieldName: String = c.Expr[String](Literal(Constant(field))).splice
        val value: String = c.Expr[String](Literal(Constant(nameValue))).splice
        private[muster] val consumer: Consumer[C] = ce.splice
        private[muster] val producer: Producer[C] = pe.splice
      }
    }
  }
}
trait TypeHint[T, C <: T] {
  def fieldName: String
  def value: String
  private[muster] def consumer: Consumer[C]
  private[muster] def producer: Producer[C]
}
