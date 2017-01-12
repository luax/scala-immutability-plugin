package specs.analysis

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
    TestUtils.expectMutability(Map(List("A") -> Utils.IsShallowImmutable)) {
      """
          class A[T] {
            val f: Mutable = new Mutable()
          }
          class Mutable {
            var f: Int = 0
          }
        """
    }
  }


  TestUtils.expectMutability(Map(List("Fn") -> Utils.IsDeeplyImmutable)) {
    """
        class Fn {
          val fn = (x: Int) => (x: Int) => x + 1
        }
      """
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("D", "Mutable") -> Utils.IsMutable, List("A") -> Utils.IsConditionallyImmutable, List("B") -> Utils.IsDeeplyImmutable)) {
      """
        trait A[T] {

        }
        trait B {

        }
        class Mutable {
          var f: Int = 0
        }
        class C extends A[B] with B {

        }
        class D extends A[Mutable] with B {
          var foo = 0
        }
        class E extends A[Mutable] with B {
          var foo = 0
        }

      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("A") -> Utils.IsConditionallyImmutable, List("Mutable") -> Utils.IsMutable, List("Shallow") -> Utils.IsShallowImmutable)) {
      """
          class A[T] {
          }
          class Mutable() {
            var fool: String = "mutable"
          }
          class Shallow {
            val a: A[Mutable] = new A()
            val b: A[Mutable] = new A()
          }
        """
    }
  }


  it should testNr in {
    TestUtils.expectMutability(Map(List("Implementation") -> Utils.IsShallowImmutable, List("Mutable") -> Utils.IsMutable)) {
      """
          type T = Mutable

          class Implementation {
            val t: T = new T
          }

          class Mutable {
            var foo: String = "mutable"
          }
       """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Implementation") -> Utils.IsShallowImmutable)) {
      """
          class Implementation {
            val t =  Seq("1", "2", "3")
            // val f: StringBuilder = null
          }
       """
    }
  }


  it should testNr in {
    TestUtils.expectMutability(Map(List("Implementation") -> Utils.IsMutable)) {
      """
        trait Implementation {
          var a =  Seq("1", "2", "3")
        }
       """
    }
  }

}
