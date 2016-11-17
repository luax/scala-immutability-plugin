class Mutable() {
  var mutableString = "foo"
  val immutableString = "secret"

  private var mutableInClassString = "bar"  // And companion object
  private[this] var mutableInThisObject = "baz"
}


object Main {
  def main(args: Array[String]): Unit = {
    val m = new Mutable
    m.mutableString = "bar"
  }
}
