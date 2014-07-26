package muster

import scala.util.control.NoStackTrace

class ParseException(msg: String) extends Throwable(msg) with NoStackTrace
class MappingException(msg: String) extends Throwable(msg) with NoStackTrace
class EndOfInput extends Throwable("end of input") with NoStackTrace