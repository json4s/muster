package muster

object Constants {

  /**
   * The state object holds the constants for indicating where in a serialization process
   * of a nested structure the current value is being read for.
   */
  object State {
    val None = 0
    val ArrayStarted = 1
    val InArray = 2
    val ObjectStarted = 3
    val InObject = 4
  }

  val HexAlphabet = "0123456789ABCDEF"
}