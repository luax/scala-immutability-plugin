class Mutable() {
  var mutableString = "foo"
  val immutableString = "secret"

  private var mutableInClassString = "bar"
  // And companion object
  private[this] var mutableInThisObject = "baz"

  def foo = {
    var bar = "baz"
  }

}

class Test {
  private var x0: Int = 0

  def x = x0

  def x_=(a: Int) = x0 = a
}


object Main {
  def main(args: Array[String]): Unit = {
    val m = new Mutable
    m.mutableString = "bar"

    val t = new Test
    println(t.x)
    t.x = 5
    println(t.x)
  }
}
