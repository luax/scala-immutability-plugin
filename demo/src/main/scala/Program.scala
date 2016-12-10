import mutables.{Graph, Point}

import scala.collection.mutable.ArrayBuffer

abstract class X {
  val x: String
  println("x is " + x.length)
}

object Y extends X {
  lazy val x = "Hello"
}

object Program {
  def main(args: Array[String]): Unit = {
    val p1 = new Point(2, 3)
    val p2 = new Point(2, 4)
    val p3 = new Point(3, 3)
    println(p1.isNotSimilar(p2))
    println(p1.isNotSimilar(p3))
    println(p1.isNotSimilar(2))

    val g = new Graph
    val n1 = g.newNode
    val n2 = g.newNode
    val n3 = g.newNode
    n1.connectTo(n2)
    n3.connectTo(n1)

    val fruits = ArrayBuffer[String]()
    fruits += "Apple"
    fruits += "Banana"
    fruits += "Orange"
    println(fruits)
  }
}
