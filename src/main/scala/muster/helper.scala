package muster

import scala.reflect.macros._
import java.util.Date
import java.sql.Timestamp

class Helper[C <: Context](val c: C) {
  import c.universe._

  private val primitiveTypes = Set[Type](c.typeOf[String], c.typeOf[Int], c.typeOf[Long], c.typeOf[Double],
          c.typeOf[Float], c.typeOf[Byte], c.typeOf[BigInt], c.typeOf[Boolean],
          c.typeOf[Short], c.typeOf[java.lang.Integer], c.typeOf[java.lang.Long],
          c.typeOf[java.lang.Double], c.typeOf[java.lang.Float], c.typeOf[BigDecimal],
          c.typeOf[java.lang.Byte], c.typeOf[java.lang.Boolean], c.typeOf[Number],
          c.typeOf[java.lang.Short], c.typeOf[Date], c.typeOf[Timestamp], c.typeOf[scala.Symbol],
          c.typeOf[java.math.BigDecimal], c.typeOf[java.math.BigInteger])

  def isPrimitive(tpe: c.Type) = primitiveTypes.exists(tpe =:= _)
  def isString(tpe: c.Type) = c.typeOf[String] =:= tpe || c.typeOf[java.lang.String] =:= tpe

  def caseClassFields(tpe: c.universe.Type): Seq[Symbol] = {
    tpe.declarations.toVector.filter{ v =>
      if (v.isTerm) {
        val t = v.asTerm
        t.isCaseAccessor && t.isVal
      } else false

    }
  }

  def isVal(v: Symbol) = v.isTerm && v.asTerm.isVal
  def isVar(v: Symbol) = v.isTerm && v.asTerm.isVar

  def vals(tpe: Type): Seq[Symbol] = tpe.members.toVector filter isVal
  def vars(tpe: Type): Seq[Symbol] = tpe.members.toVector filter isVar

  def isCaseClass(sym: c.universe.Symbol) = sym.isClass && sym.asClass.isCaseClass

}