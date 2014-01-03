package muster

import org.joda.time.DateTime
import java.util.Date

case class Friend(id: Int, name: String)
case class Person(
                   id: Int,
                   guid: String,
                   isActive: Boolean,
                   age: Int,
                   gender: String,
                   company: String,
                   email: String,
                   about: String,
                   registered: DateTime,
                   latitude: Double,
                   longitude: Double,
                   tags: Set[String],
                   friends: Seq[Friend],
                   randomArrayItem: String)

case class Simple(one: Int, two:String)
case class WithOption(one: Int, two: Option[String])
case class WithOptionSimple(option: Option[Simple])
case class NotSimple(one: Int, simple: Simple)
case class WithList(lst: List[Int])
case class ObjWithListMap(lst: List[Int], map: Map[String,Int])
case class WithDate(date: Date)
case class WithDateTime(date: DateTime)
case class WithSymbol(symbol: Symbol)

case class Junk(in1:Int, in2:String)
case class MutableJunk(var in1:Int,var in2:String)
case class MutableJunkWithField(var in1:Int) {
  var in2:String = _
}
case class MutableJunkWithJunk(var in1: Int) {
  var in2: Junk = Junk(0, "")
}
case class ThingWithJunk(name:String, junk:Junk)
case class Crazy(name:String,thg:ThingWithJunk)
case class OptionOption(in:Option[Option[Int]])
case class JunkWithDefault(in1:Int, in2:String="Default...")
case class WithListAndName(name:String, lst:List[Int])
case class WithObjList(name:String, list:List[ThingWithJunk])
case class Curried(in1:Int,in2:Int)(in3:Int)
case class WithTpeParams[U](in1:U)
class WithNstedTpeParams[U,U2](val in1: U, val in2:WithTpeParams[U2]) {
  override def equals(in:Any) = in match {
    case in:WithNstedTpeParams[U,U2] => in.in1 == in.in1 && in.in2 == in2
	case _ => false
  }
}
case class ResolvedParams[U](in3: U, in4:WithTpeParams[Int]) extends WithNstedTpeParams[U,Int](in3,in4)
case class Bill(in:Int)
case class WithSeq(in: Seq[Int])

class Billy[U](in:U)
case class BillyB(in:Int) extends Billy[Int](in)

case class WithDateAndName(name:String, date: Date) {
  override def equals(in:Any) = in match {
    case that: WithDateAndName => name == that.name && date.toString == that.date.toString
	case _ => false
  }
}

class ClassWithDef(val in: Int=4) {
  override def toString = s"ClassWithDef(in:$in)"
  override def equals(obj:Any) = obj match {
	  case a:ClassWithDef => a.in == in
	  case _ => false
  }
}


case class ObjWithDefJunk(name:String, junk:Junk=Junk(-1,"Default"))