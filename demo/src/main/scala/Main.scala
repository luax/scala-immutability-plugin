class Point(xc: Int, yc: Int) {
  var x: Int = xc
  var y: Int = yc
  def move(dx: Int, dy: Int) {
    x = x + dx
    y = y + dy
  }
  override def toString(): String = "(" + x + ", " + y + ")"
}

object Classes {
  def main(args: Array[String]) {
    val pt = new Point(1, 2)
    println(pt)
    pt.move(10, 10)
    println(pt)

    val m = new Mutable("foo")
    val m2 = new MutableBuddy()
    val i = new Immutable("foo")
    val i2 = new ImmutableBuddy()
  }
}

