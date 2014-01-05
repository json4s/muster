package muster

import scala.reflect.macros._
import java.util.Date
import java.sql.Timestamp
import scala.annotation.tailrec

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
    tpe.declarations.toVector.filter {
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

  def isVal(v: Symbol) = v.isTerm && v.asTerm.isVal && v.isPublic

  def isVar(v: Symbol) = v.isTerm && v.asTerm.isVar && v.isPublic

  def vals(tpe: Type): Seq[Symbol] = tpe.members.toVector filter isVal

  def vars(tpe: Type): Seq[Symbol] = tpe.members.toVector filter isVar

  //  def isCaseClass(sym: c.universe.Symbol) = sym.isClass && sym.asClass.isCaseClass
  def isCaseClass(tpe: Type) = {
    val sym = tpe.typeSymbol
    sym.isClass && sym.asClass.isCaseClass
  }

  def isOption(tpe: Type) = tpe <:< typeOf[Option[_]]

  def isEither(tpe: Type) = tpe <:< typeOf[Either[_, _]]

  def isMap(tpe: Type) = tpe <:< typeOf[scala.collection.GenMap[_, _]]

  def isSeq(tpe: Type) = tpe <:< typeOf[scala.collection.GenSeq[_]]

  def isSet(tpe: Type) = tpe <:< typeOf[scala.collection.GenSet[_]]

  def typeArgumentTree(t: c.Type): c.Tree = t match {
    case TypeRef(_, _, typeArgs@_ :: _) =>
      AppliedTypeTree(Ident(t.typeSymbol), typeArgs map (t => typeArgumentTree(t)))
    case _ =>
      Ident(t.typeSymbol.name)
  }

  private[this] def pickConstructor(tpe: Type, argNames: Set[String]): (MethodSymbol, Seq[Symbol]) = {
    val clazz = tpe.typeSymbol.asClass
    val withAlt = clazz.typeSignature.member(nme.CONSTRUCTOR).asTerm.alternatives
    val ctors = withAlt.map(_.asMethod).sortBy(-_.paramss.sortBy(-_.size).headOption.getOrElse(Nil).size)
    val zipped = ctors zip (ctors map (ctor => pickConstructorArgs(ctor.paramss, argNames)))
    zipped collectFirst {
      case (m: MethodSymbol, Some(args)) =>
        (m, args)
    } getOrElse (throw new RuntimeException(s"Couldn't find a constructor for ${clazz.name.decoded.trim} and args: [${argNames.mkString(", ")}]"))
  }

  private[this] def pickConstructorArgs(candidates: Seq[Seq[Symbol]], argNames: Set[String]): Option[Seq[Symbol]] = {
    val ctors = candidates.sortBy(-_.size)
    def isRequired(item: Symbol) = {
      val sym = item.asTerm
      !(sym.isParamWithDefault || sym.typeSignature <:< typeOf[Option[_]])
    }
    def matchingRequired(plist: Seq[Symbol]) = {
      val required = plist filter isRequired
      required.size <= argNames.size && required.forall(s => argNames.contains(s.name.decoded.trim))
    }
    ctors find matchingRequired
  }

  def getNonConstructorVars(tpe: Type): Seq[Symbol] = {
    // Make sure that the param isn't part of the constructor
    val ctorParams = tpe.member(nme.CONSTRUCTOR).asTerm.alternatives
      .map(_.asMethod.paramss.flatten.map(_.name.toTermName.toString.trim))
      .flatten
      .toSet

    // TODO: Looks like these are always accessed with getters and setters. Need to find if the getters and setters
    //       are valid
    for {
    // TODO: need to check if the var is public or not, but doesn't seem to work properly
      sym <- vars(tpe) if !ctorParams.exists(sym.name.toTermName.toString.trim == _)
    } yield sym
  }

  def getJavaStyleSetters(tpe: Type) = {
    val candidates = tpe.members.filter(_.isTerm).filter(_.asTerm.isMethod).filter { s =>
      val name = s.asTerm.name.decoded
      name.startsWith("get") || name.startsWith("set")
    }
    candidates.filter(sym =>
      candidates.exists(_.asTerm.name.decoded.trim.endsWith(sym.asTerm.name.decoded.trim.substring("set".length))) &&
        sym.asMethod.paramss.flatten.length == 1
    ).toList
  }
}