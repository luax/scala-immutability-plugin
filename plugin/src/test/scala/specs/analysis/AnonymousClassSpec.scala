package specs.analysis

import helpers.Utils
import org.scalatest._
import utils.TestUtils

class AnonymousClassSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Fn") -> Utils.IsDeeplyImmutable)) {
      """
      class Fn {
        val fn = (x: Int) => x + 1
      }
      """
    }

    TestUtils.expectMutability(Map(List("Fn") -> Utils.IsDeeplyImmutable)) {
      """
      class Fn {
        val fn = (x: Int) => (x: Int) => x + 1
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Test") -> Utils.IsDeeplyImmutable)) {
      """
      trait B {}
      class A {}
      class Test {
        val fn = new A with B
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Test", "A") -> Utils.IsShallowImmutable, List("B") -> Utils.IsMutable)) {
      """
      trait B {
        var a: String = "mutable"
      }
      class A {
         val b: B = null
      }
      class Test {
        val fn = new A with B
      }
      """
    }

    TestUtils.expectMutability(Map(List("Test", "A") -> Utils.IsShallowImmutable, List("B") -> Utils.IsMutable)) {
      """
      trait B {
        var a: String = "mutable"
      }
      class Test {
        val fn = new A with B
      }
      class A {
         val b: B = null
      }

      """
    }

    TestUtils.expectMutability(Map(List("Test", "A") -> Utils.IsShallowImmutable, List("B") -> Utils.IsMutable)) {
      """
      class Test {
        val fn = new A with B
      }
      trait B {
        var a: String = "mutable"
      }
      class A {
         val b: B = null
      }
      """
    }
  }
}
