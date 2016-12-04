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
    TestUtils.expectMutability("A", Utils.IsMutable) {
      """
      class A(var foo: String)
      """
    }
  }
}
