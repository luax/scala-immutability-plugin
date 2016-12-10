package mutables

;

import scala.collection.mutable.ArrayBuffer

class StandardLibrary {
  val fruits = ArrayBuffer[String]()

  def foo(): Unit = {
    fruits += "Apple"
    fruits += "Banana"
    fruits += "Orange"
  }

  def bar(): Unit = {
    val fruits = ArrayBuffer[String]()
    fruits += "Apple"
    fruits += "Banana"
    fruits += "Orange"
    println(fruits)
  }
}
