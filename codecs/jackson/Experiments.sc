//import muster.codec.json.api.{ JsonFormat => fmt }
import muster._
import scala.reflect.runtime.{universe => u}
import u._
val obj = WithTpeParams(393040)
val tpe = typeOf[WithTpeParams[Int]]
val fn = tpe.member(termNames.CONSTRUCTOR).asTerm.alternatives.map(_.asMethod.paramLists.flatten.map(_.name)).flatten.toSet.head
val fld = tpe.members.find(a => a.name == fn).get
val obj2 = Bill(203)
val tpe2 = typeOf[Bill]
val fn2 = tpe2.member(termNames.CONSTRUCTOR).asTerm.alternatives.map(_.asMethod.paramLists.flatten.map(_.name)).flatten.toSet.head
val fld2 = tpe2.members.find(a => a.name == fn2).get
println(fld)
//val fn2 = tpe2.member(termNames.CONSTRUCTOR).asTerm.alternatives.map(_.asMethod.paramLists.flatten.map(_.name)).flatten.toSet.head
//val fld2 = tpe.members.find(a => a.name == fn2)

