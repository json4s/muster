package muster

/** Provides a way to rename fields, needs to be in scope for the (de)serializer to pick it up.
  *
  * @tparam T invariant type param to hook these renames to
  */
trait FieldRenames[T] {
  def renameInput(fieldName: String) // When reading
  def renameOutput(fieldName: String) // When writing
}
