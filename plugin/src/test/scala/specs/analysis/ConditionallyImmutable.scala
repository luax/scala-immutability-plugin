package specs.analysis

import helpers.Utils
import org.scalatest.FlatSpec
import utils.TestUtils

class ConditionallyImmutable extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Foo") -> Utils.IsConditionallyImmutable, List("Mutable") -> Utils.IsMutable, List("A", "Immutable") -> Utils.IsDeeplyImmutable, List("B") -> Utils.IsShallowImmutable)) {
      """
        class A {
          val a = new Foo[Immutable]()
        }
        class B {
          val a = new Foo[Mutable]()
        }
        class Mutable {
          var mutable: String = "foo"
        }
        class Immutable {}
        class Foo[T] {
        }
      """
    }
    TestUtils.expectMutability(Map(List("Foo") -> Utils.IsConditionallyImmutable, List("Mutable", "B") -> Utils.IsMutable, List("A", "Immutable") -> Utils.IsDeeplyImmutable)) {
      """
        class A extends Foo[Immutable] {
        }
        class B extends Foo[Mutable] {
        }
        class Mutable {
          var mutable: String = "foo"
        }
        class Immutable {}
        class Foo[T] {
        }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("A") -> Utils.IsConditionallyImmutable, List("Mutable", "E", "F", "G") -> Utils.IsMutable, List("Immutable", "C") -> Utils.IsDeeplyImmutable, List("H", "I") -> Utils.IsShallowImmutable)) {
      """
      import secret._
      class Mutable {
        var a: Int = 0
      }
      class Immutable {
        val a: Int = 0
      }
      class A[T] {
      }
      class C {
        val a: A[Int] = null // immutable
      }
      class D {
        val a: A[Immutable] = null  // immutable
      }
      class E {
        var a: A[Int] = null  // mutable
      }
      class F {
        var a: A[Mutable] = null  // mutable
      }
      class G {
        var a: A[secret.SecretClass] = null  // mutable
      }
      class H {
        val a: A[secret.SecretClass] = null  // shallow
      }
      class I {
        val a: A[Mutable] = null  // shallow
      }
      """
    }
    // Different position of A
    TestUtils.expectMutability(Map(List("A") -> Utils.IsConditionallyImmutable, List("Mutable", "E", "F", "G") -> Utils.IsMutable, List("Immutable", "C") -> Utils.IsDeeplyImmutable, List("H", "I") -> Utils.IsShallowImmutable)) {
      """
          import secret._
          class Mutable {
            var a: Int = 0
          }
          class Immutable {
            val a: Int = 0
          }
          class A[T] {
          }
          class C {
            val a: A[Int] = null // immutable
          }
          class D {
            val a: A[Immutable] = null  // immutable
          }
          class E {
            var a: A[Int] = null  // mutable
          }
          class F {
            var a: A[Mutable] = null  // mutable
          }
          class G {
            var a: A[secret.SecretClass] = null  // mutable
          }
          class H {
            val a: A[secret.SecretClass] = null  // shallow
          }
          class I {
            val a = new A[Mutable]()  // shallow
          }
          """
    }

  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Mutable", "B", "E", "F", "G") -> Utils.IsMutable, List("C", "D", "H", "I", "J") -> Utils.IsShallowImmutable, List("Immutable") -> Utils.IsDeeplyImmutable)) {
      """
        import secret._
        class Mutable {
          var a: Int = 0
        }
        class Immutable {
          val a: Int = 0
        }
        class B[T] {
          var t: Int = 0 // mutable
        }
        class C {
          val a: B[Int] = null  // shallow
        }
        class D {
          val a: B[Immutable] = null  // shallow
        }
        class E {
          var a: B[Int] = null  // mutable
        }
        class F {
          var a: B[Mutable] = null  // mutable
        }
        class G {
          var a: B[secret.SecretClass] = null  // mutable
        }
        class H {
          val a: B[secret.SecretClass] = null  // shallow
        }
        class I {
          val a: B[Mutable] = null  // shallow
        }
        class J {
          val a: B[Mutable] = null  // shallow
        }
        """
    }
    // Different position of B
    TestUtils.expectMutability(Map(List("Mutable", "B", "E", "F", "G") -> Utils.IsMutable, List("C", "D", "H", "I", "J") -> Utils.IsShallowImmutable, List("Immutable") -> Utils.IsDeeplyImmutable)) {
      """
        import secret._
        class Mutable {
          var a: Int = 0
        }
        class Immutable {
          val a: Int = 0
        }
        class C {
          val a: B[Int] = null  // shallow
        }
        class D {
          val a: B[Immutable] = null  // shallow
        }
        class E {
          var a: B[Int] = null  // mutable
        }
        class F {
          var a: B[Mutable] = null // mutable
        }
        class G {
          var a: B[secret.SecretClass] = null  // mutable
        }
        class H {
          val a: B[secret.SecretClass] = null  // shallow
        }
        class I {
          val a: B[Mutable] = null  // shallow
        }
        class B[T] {
          var t: Int = 0 // mutable
        }
        class J {
          val a: B[Mutable] = null  // shallow
        }
        """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("D", "E", "H", "Mutable", "I", "J", "B", "L", "MutableTrait") -> Utils.IsMutable, List("F", "G", "A") -> Utils.IsConditionallyImmutable, List("C", "Immutable") -> Utils.IsDeeplyImmutable)) {
      """
        import secret._

        class C extends A[Immutable] {
          // C is immutable
          val immutable: String = "foo"
        }

        class D extends A[Mutable] {
          // D is mutable
          val immutable: String = "foo"
        }

        class E extends A[Mutable] {
          // E is mutable
          var mutable: String = "foo"
        }

        class Immutable {
          // Immutable
          val a: Int = 0
        }

        class F[T] extends A[T] {
          // F is Conditionally Immutable
          val mutable: String = "foo"
        }

        class G[T] extends A[T] {
          // G is Conditionally Immutable
          val immutable: String = "foo"
        }

        class H[T] extends A[T] {
          // H is mutable
          var mutable: String = "foo"
        }

        class Mutable {
          // Mutable
          var a: Int = 0
        }

        class I extends A[Immutable] with MutableTrait {
          // I is mutable
        }

        class J extends B[Immutable] {
          // J is Mutable
        }

        class B[T] {
          // Mutable
          var t: Int = 0
        }

        class A[T] {
          // Conditionally immutable
        }

        class L extends A[secret.SecretClass] {
          // L is Mutable
        }

        trait MutableTrait {
          // Mutable
          var b: Int = 0
        }
        """
    }
  }

}
