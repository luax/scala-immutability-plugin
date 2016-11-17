package helpers

object Log {
  def log(msg: => String): Unit = {
    println(msg)
  }
}
