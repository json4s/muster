//package muster
//
//import scala.language.experimental.macros
//import scala.reflect.macros._
//
//
//object TypeHints {
//  def apply[T](fName: String) = new TypeHints[T] { val fieldName: String = fName }
//}
//
//trait TypeHints[T] {
//  def fieldName: String
//
//  def short[C <: T](): TypeHint[C] = macro shortImpl[C]
//  def shortImpl[C <: T: c.WeakTypeTag](c: blackbox.Context): c.Expr[TypeHint[C]] = {
//    val tpe = c.weakTypeOf[C].dealias
//    generate[C, c.type](c)(fieldName, tpe.typeSymbol.name.decodedName.toString, tpe)
//  }
//
//  def full[C <: T](): TypeHint[C] = macro fullImpl[C]
//  def fullImpl[C <: T: c.WeakTypeTag](c: blackbox.Context): c.Expr[TypeHint[C]] = {
//    val tpe = c.weakTypeOf[C].dealias
//    generate[C, c.type](c)(fieldName, tpe.typeSymbol.fullName, tpe)
//  }
//
//  def custom[C <: T](stringValue: String): TypeHint[C] = macro customImpl[C]
//  def customImpl[C <: T: c.WeakTypeTag](c: blackbox.Context)(stringValue: c.Expr[String]): c.Expr[TypeHint[C]] = {
//    val tpe = c.weakTypeOf[C].dealias
//    generate[C, c.type](c)(fieldName, stringValue.splice, tpe)
//  }
//
//  private def generate[C <: T : c.WeakTypeTag, CT <: blackbox.Context](c: CT)(field: String, nameValue: String, tpe: c.Type): c.Expr[TypeHint[C]] = {
//    import c.universe._
//    val fn = tpe.typeSymbol.fullName
//    val ct = appliedType(typeOf[Consumer[Any]], tpe :: Nil)
//    val pt = appliedType(typeOf[Producer[Any]], tpe :: Nil)
//    val ce =  c.inferImplicitValue(ct) match {
//      case EmptyTree =>
//        c.abort(c.enclosingPosition, s"Couldn't find a muster.Consumer[$fn], try bringing an implicit value for ${ct.typeSymbol.fullName} in scope by importing one or defining one.")
//      case resolved => c.Expr[Consumer[C]](resolved)
//    }
//    val pe = c.inferImplicitValue(pt) match {
//      case EmptyTree =>
//        c.abort(c.enclosingPosition, s"Couldn't find a muster.Producer[$fn], try bringing an implicit value for ${pt.typeSymbol.fullName} in scope by importing one or defining one.")
//      case resolved => c.Expr[Producer[C]](resolved)
//    }
//
//    reify {
//      new TypeHint[T] {
//        val fieldName: String = c.Expr[String](Literal(Constant(field))).splice
//        val value: String = c.Expr[String](Literal(Constant(nameValue))).splice
//        private[muster] val consumer: Consumer[C] = ce.splice
//        private[muster] val producer: Producer[C] = pe.splice
//      }
//    }
//  }
//
//
//  trait TypeHint[C <: T] {
//    def value: String
//    def consumer: Consumer[C]
//    def producer: Producer[C]
//  }
//}
