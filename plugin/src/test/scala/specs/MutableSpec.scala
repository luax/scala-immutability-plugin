package specs

import helpers.Utils
import org.scalatest._
import utils.TestUtils

class MutableSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  it should testNr in {
    TestUtils.expectMutability("A", Utils.IsMutable) {
      """
      class A {
        var foo: String = "mutate me"
      }
      """
    }
    TestUtils.expectMutability("A", Utils.IsMutable) {
      """
      class A(var foo: String)
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(List("A", "B"), Utils.IsMutable) {
      """
      class A {
        var foo: String = "mutate me"
        val bar: String = "immutable"
        val baz: String = "immutable"
      }
      class B {
        val bar: String = "immutable"
        val baz: String = "immutable"
        var foo: String = "mutate me"
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(List("A", "B", "C", "D"), Utils.IsMutable) {
      """
      class A {
        var foo: B = new B
        class B {
          var bar: C = new C
          class C {
            var baz: D = new D
            class D {
              var test: Int = 0
            }
          }
        }
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability("A", Utils.IsMutable) {
      """
      object A {
        var foo: String = "foo"
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability("A", Utils.IsMutable) {
      """
      trait A {
        var foo: String = "foo"
      }
      """
    }
  }
}
