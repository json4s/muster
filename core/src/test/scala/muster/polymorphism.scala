package muster

trait Animal
case class Dog(limbs: Int, name: String, id: Long) extends Animal
case class Fish(name: String, id: Long, gils: Int) extends Animal
case class Snake(name: String, id: Long, length: Int) extends Animal

case class Habitat[T <: Animal](animal: T)
case class Inventory[T <: Animal](animals: List[T])