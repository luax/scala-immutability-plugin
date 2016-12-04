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
    TestUtils.expectMutability(List("A", "B"), Utils.IsDeeplyImmutable) {
      """
      class B extends A {
        val x: String = "immutable"
      }
      class A {
        val foo: String = "immutable"
        val bar: String = "immutable"
        val baz: String = "immutable"
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
    TestUtils.expectMutability(List("A", "B"), Utils.IsDeeplyImmutable) {
      """
      class A {
        val foo: B = new B
      }
      class B {
        val foo: String = "immutable"
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(List("A", "B", "C", "D"), Utils.IsDeeplyImmutable) {
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
