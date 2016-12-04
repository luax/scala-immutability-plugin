package specs

import helpers.Utils
import org.scalatest._
import utils.TestUtils

class ImmutableSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  it should testNr in {
    TestUtils.expectMutability(List("A", "B"), Utils.IsDeeplyImmutable) {
      """
      class A {
        val foo: String = "immutable"
        val bar: String = "immutable"
        val baz: String = "immutable"
      }
      class B extends A {
        val x: String = "immutable"
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability("A", Utils.IsDeeplyImmutable) {
      """
      class A(val foo: String)
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(List("A", "B"), Utils.IsDeeplyImmutable) {
      """
      class B {
        val foo: String = "immutable"
      }

      class A {
        val foo: B = new B
      }
      """
    }
  }
}
