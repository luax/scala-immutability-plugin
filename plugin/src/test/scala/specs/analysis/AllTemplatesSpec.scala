package specs.analysis

import helpers.Utils
import org.scalatest._
import utils.TestUtils

class AllTemplatesSpec extends FlatSpec {

  val templates =
    """
  class A {
    var a: Int = 100
  }
  class B {
    val a: Int = 100
  }
  class C {
    lazy val a: Int = 100
  }
  case class D(var a: Int = 100)
  case class E(val a: Int = 100)
  abstract class F {
    var a: Int = 100
  }
  abstract class G {
    val a: Int = 100
  }
  abstract class H {
    lazy val a: Int = 100
  }
  trait I {
    var a: Int = 100
  }
  trait J {
    val a: Int = 100
  }
  trait K {
    lazy val a: Int = 100
  }
  object L {
    var a: Int = 100
  }
  object M {
    val a: Int = 100
  }
  object N {
    lazy val a: Int = 100
  }
    """
  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("A", "D", "F", "I", "L") -> Utils.IsMutable, List("B", "C", "E", "G", "H", "J", "K", "N") -> Utils.IsDeeplyImmutable)) {
      templates
    }
  }
}
