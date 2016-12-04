package specs

import helpers.Utils
import org.scalatest._
import utils.TestUtils

class DevelopmentSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }


  it should testNr in {
    TestUtils.expectMutability(List("A", "B"), Utils.IsDeeplyImmutable) {
      """
      class A {
        val foo: B = new B
        class B {
          val bar: C = new C
          class C {
            val baz: D = new D
            class D {
              val test: Int = 0
            }
          }
        }
      }
      """
    }
  }
}
