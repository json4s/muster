package muster

/** Formats output for use when a producer renders to a producible
  *
  * @example A better toString for case classes
  *          {{{
  *            class StringOutputFormatter(val writer: Appendable[_], quoteStringWith: String = "\"", escapeSpecialChars: Boolean = true) extends OutputFormatter[String] {
  *
  *              import Constants._
  *
  *              protected val stateStack = mutable.Stack[Int]()
  *
  *              protected def state = stateStack.headOption getOrElse State.None
  *
  *              def startArray(name: String) {
  *                writeComma(State.InArray)
  *                writer.append(name)
  *                writer.append('(')
  *                stateStack push State.ArrayStarted
  *              }
  *
  *              def endArray() {
  *                writer.append(')')
  *                stateStack.pop()
  *              }
  *
  *              def startObject(name: String) {
  *                writeComma(State.InArray)
  *                writer.append(name)
  *                writer.append('(')
  *                stateStack push State.ObjectStarted
  *              }
  *
  *              def endObject() {
  *                writer.append(')')
  *                stateStack.pop()
  *              }
  *
  *              def string(value: String) {
  *                writeComma(State.InArray)
  *                if (quoteStringWith != null && quoteStringWith.trim.nonEmpty) writer.append(quoteStringWith)
  *                if (escapeSpecialChars) Quoter.jsonQuote(value, writer) else writer.append(value)
  *                if (quoteStringWith != null && quoteStringWith.trim.nonEmpty) writer.append(quoteStringWith)
  *              }
  *
  *              def byte(value: Byte) {
  *                writeComma(State.InArray)
  *                writer.append(value.toString)
  *              }
  *
  *              def int(value: Int) {
  *                writeComma(State.InArray)
  *                writer.append(value.toString)
  *              }
  *
  *              def long(value: Long) {
  *                writeComma(State.InArray)
  *                writer.append(value.toString)
  *              }
  *
  *              def bigInt(value: BigInt) {
  *                writeComma(State.InArray)
  *                writer.append(value.toString)
  *              }
  *
  *              def boolean(value: Boolean) {
  *                writeComma(State.InArray)
  *                writer.append(value.toString)
  *              }
  *
  *              def short(value: Short) {
  *                writeComma(State.InArray)
  *                writer.append(value.toString)
  *              }
  *
  *              def float(value: Float) {
  *                writeComma(State.InArray)
  *                writer.append(value.toString)
  *              }
  *
  *              def double(value: Double) {
  *                writeComma(State.InArray)
  *                writer.append(value.toString)
  *              }
  *
  *              def bigDecimal(value: BigDecimal) {
  *                writeComma(State.InArray)
  *                writer.append(value.toString)
  *              }
  *
  *              def writeNull() {
  *                writeComma(State.InArray)
  *                writer.append("null")
  *              }
  *
  *              def undefined() {}
  *
  *              private[this] def writeComma(when: Int*) {
  *                if (state == State.ArrayStarted) {
  *                  stateStack.pop()
  *                  stateStack push State.InArray
  *                } else if (state == State.ObjectStarted) {
  *                  stateStack.pop()
  *                  stateStack push State.InObject
  *                } else if (when contains state) {
  *                  writer.append(',')
  *                  writer.append(' ')
  *                }
  *              }
  *
  *              def startField(name: String) {
  *                writeComma(State.InObject, State.InArray)
  *                writer.append(name.trim)
  *                writer.append(':')
  *                writer.append(' ')
  *              }
  *
  *              def result: String = writer.toString
  *
  *              def close() {
  *                writer.close()
  *              }
  *
  *            }
  *          }}}
  *
  * @tparam R the type of value this formatter produces
  */
trait OutputFormatter[R] extends AutoCloseable {
  /** Push the start of an array onto the stream
    *
    * @param name the label to use
    */
  def startArray(name: String = ""): Unit

  /** Exit array push mode */
  def endArray(): Unit

  /** Push the start of a complex object onto the stream
    *
    * @param name
    */
  def startObject(name: String = ""): Unit

  /** End object mode */
  def endObject(): Unit

  /** Push the a string */
  def string(value: String): Unit

  /** Push the a byte number */
  def byte(value: Byte): Unit

  /** Push the an int number */
  def int(value: Int): Unit

  /** Push the a long number */
  def long(value: Long): Unit

  /** Push the a big int number */
  def bigInt(value: BigInt): Unit

  /** Push the a boolean */
  def boolean(value: Boolean): Unit

  /** Push the a short number */
  def short(value: Short): Unit

  /** Push the a float number */
  def float(value: Float): Unit

  /** Push the a double number */
  def double(value: Double): Unit

  /** Push the a big decimal number */
  def bigDecimal(value: BigDecimal): Unit

  /** Push the field name if we're currently in object mode */
  def startField(name: String): Unit

  /** Push the a null value */
  def writeNull(): Unit

  /** discard field value if any */
  def undefined(): Unit

  /** the result of the formatting operation, in the case of streams this will be [[scala.Unit]] */
  def result: R

  /** close the underlying resources if any */
  def close()
}

/** Renders a value through a [[muster.Producer]] into a [[muster.Producible]]
  *
  * @tparam R the type of value for the formatter in this renderer
  */
trait Renderer[R] {
  /** the type of formatter this renderer supports */
  type Formatter <: OutputFormatter[R]

  /** Create a formatter */
  def createFormatter: Formatter

  /** Perform the rendering
    *
    * @param out the subject to render
    * @param producer the [[muster.Producer]] for [[T]]
    * @tparam T the type of subject to render
    * @return the result of the rendering, as dictacted by the formatter
    */
  def from[T](out: T)(implicit producer: Producer[T]): R = {
    val fmt = createFormatter
    producer.produce(out, fmt)
    fmt.result
  }
}
