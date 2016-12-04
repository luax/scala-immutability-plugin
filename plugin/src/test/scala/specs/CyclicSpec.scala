package specs

import helpers.Utils
import org.scalatest._
import utils.TestUtils

class CyclicSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  it should testNr in {
    TestUtils.expectMutability(List("A", "B"), Utils.IsMutable) {
      """
      class A {
        var b: B = new B
      }
      class B {
        var a: A = new A
      }
      """
    }
    TestUtils.expectMutability(List("A", "B"), Utils.IsMutable) {
      """
      class B {
        var a: A = new A
      }
      class A {
        var b: B = new B
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("B") -> Utils.IsShallowImmutable, List("A") -> Utils.IsMutable)) {
      """
      class A {
        var b: B = new B
      }
      class B {
        val a: A = new A
      }
      """
    }
    TestUtils.expectMutability(Map(List("B") -> Utils.IsShallowImmutable, List("A") -> Utils.IsMutable)) {
      """
      class B {
        val a: A = new A
      }
      class A {
        var b: B = new B
      }
      """
    }
  }
}
