package muster

import scala.language.experimental.macros
import scala.reflect.macros._
import java.util.Date
import java.sql.Timestamp
import scala.annotation.tailrec

class Helper[C <: blackbox.Context](val c: C) {
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
    tpe.decls.toVector.filter {
      v =>
        if (v.isTerm) {
          val t = v.asTerm
          t.isCaseAccessor && t.isVal
        } else false

    }
  }

  def resolveInnerOptionType(tpe: Type): Type = {
    @tailrec def fetchType(tp: Type): Type = {
      if (tp <:< weakTypeOf[Option[_]]) {
        val TypeRef(_, _, agTp :: Nil) = tp
        fetchType(agTp)
      } else tp
    }
    fetchType(tpe)
  }

  def isVal(v: Symbol) = v.isTerm && v.asTerm.isVal && (v.isPublic||v.asTerm.isCaseAccessor)

  def isVar(v: Symbol) = v.isTerm && v.asTerm.isVar && !v.asTerm.isCaseAccessor

  def vals(tpe: Type): Seq[Symbol] = tpe.members.toVector filter isVal

  def vars(tpe: Type): Seq[Symbol] = tpe.members.toVector filter isVar

  def isCaseClass(tpe: Type) = {
    val sym = tpe.typeSymbol
    sym.isClass && sym.asClass.isCaseClass
  }

  def isOption(tpe: Type) = tpe <:< typeOf[Option[_]]

  def isEither(tpe: Type) = tpe <:< typeOf[Either[_, _]]

  def isMap(tpe: Type) = tpe <:< typeOf[scala.collection.GenMap[_, _]] || tpe <:< typeOf[java.util.Map[_, _]]

  def isSeq(tpe: Type) = tpe <:< typeOf[scala.collection.GenSeq[_]] || tpe <:< typeOf[java.util.List[_]]

  def isSet(tpe: Type) = tpe <:< typeOf[scala.collection.GenSet[_]] || tpe <:< typeOf[java.util.Set[_]]

  def typeArgumentTree(t: c.Type): c.Tree = t match {
    case TypeRef(_, _, typeArgs@_ :: _) =>
      AppliedTypeTree(Ident(t.typeSymbol), typeArgs map (t => typeArgumentTree(t)))
    case _ =>
      Ident(t.typeSymbol.name)
  }

  def isJavaOrScalaSetter(varNames: Set[String], v: Symbol) = {
    val methodName = v.name.decodedName.toString.trim
    v.isTerm && v.asTerm.isMethod && v.isPublic &&
      (v.asTerm.asMethod.isSetter || v.asTerm.name.decodedName.toString.startsWith("set")) &&
      varNames.exists(vn => s"set${vn.capitalize}" == methodName || s"${vn}_=" == methodName) &&
      v.asMethod.paramLists.flatten.length == 1
  }

  def getSetters(tpe: Type): List[Symbol] = {
    val ctorParams = tpe.member(termNames.CONSTRUCTOR).asTerm.alternatives.map(_.asMethod.paramLists.flatten.map(_.name)).flatten.toSet
    val varNames = vars(tpe).filter(sym => sym.asTerm.isProtected || sym.asTerm.isPrivate && !ctorParams(sym.name)).map(_.name.decodedName.toString.trim).toSet
    (tpe.members filter (isJavaOrScalaSetter(varNames, _))).toList
  }

  def isJavaOrScalaGetter(varNames: Set[String], v: Symbol) = {
    val methodName = v.name.decodedName.toString.trim
    v.isTerm && v.asTerm.isMethod && v.isPublic &&
      (v.asTerm.asMethod.isGetter || v.asTerm.name.decodedName.toString.startsWith("get")) &&
      varNames.exists(vn => s"get${vn.capitalize}" == methodName || vn == methodName) &&
      v.asMethod.paramLists.flatten.length == 0
  }

  def getGetters(tpe: Type): List[Symbol] = {
    val ctorParams = tpe.member(termNames.CONSTRUCTOR).asTerm.alternatives.map(_.asMethod.paramLists.flatten.map(_.name)).flatten.toSet
    val valNames = vals(tpe).filterNot(sym => sym.asTerm.isProtected || sym.asTerm.isPrivate ).map(_.name.decodedName.toString.trim).toSet
    val varNames = vars(tpe).filter(sym => sym.asTerm.isProtected || sym.asTerm.isPrivate).map(_.name.decodedName.toString.trim).toSet
    (tpe.members filter (isJavaOrScalaGetter(varNames ++ valNames ++ ctorParams.map(_.decodedName.toString.trim), _))).toList
  }


}