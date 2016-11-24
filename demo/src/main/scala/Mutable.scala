import java.util.concurrent.CountDownLatch

import lattice.{DefaultKey, Lattice, LatticeViolationException, NaturalNumberKey, NaturalNumberLattice}
import cell.{Cell, CellCompleter, HandlerPool}

import scala.util.{Failure, Success, Try}

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

    val intLattice: Lattice[Int] = new NaturalNumberLattice

    val latch = new CountDownLatch(1)

    val pool = new HandlerPool
    pool.execute { () =>
      val completer = CellCompleter[DefaultKey[Int], Int](pool, new DefaultKey[Int])(intLattice)
      val cell = completer.cell
      cell.onComplete {
        case Success(v) =>
          latch.countDown()
        case Failure(e) =>
          latch.countDown()
      }
      completer.putFinal(5)
    }
    latch.await()
    pool.onQuiescent { () =>
      println("quiescent")
    }
    pool.shutdown()

  }
}
