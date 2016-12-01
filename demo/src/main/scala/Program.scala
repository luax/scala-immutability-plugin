import mutables.{Point, Graph}

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
  }
}
