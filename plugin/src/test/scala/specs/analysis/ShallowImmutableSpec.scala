package specs.analysis

import helpers.Utils
import org.scalatest._
import utils.TestUtils

class ShallowImmutableSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Immutable") -> Utils.IsShallowImmutable, List("Mutable") -> Utils.IsMutable)) {
      """
      class Immutable {
        val immutable: Mutable = new Mutable
        class Mutable {
          var mutable: Int = 0
        }
      }
      """
    }
    TestUtils.expectMutability(Map(List("Immutable") -> Utils.IsShallowImmutable, List("Mutable") -> Utils.IsMutable)) {
      """
      class Immutable {
        class Mutable {
          var mutable: Int = 0
        }
        val immutable: Mutable = new Mutable
      }
      """
    }
  }


  it should testNr in {
    TestUtils.expectMutability(Map(List("ShallowImmutable") -> Utils.IsShallowImmutable, List("Mutable") -> Utils.IsMutable)) {
      """
      class Mutable {
        var mutable: Int = 0
      }
      class ShallowImmutable {
        val shallowImmutable: Mutable = new Mutable
      }
      """
    }

    TestUtils.expectMutability(Map(List("ShallowImmutable") -> Utils.IsShallowImmutable, List("Mutable") -> Utils.IsMutable)) {
      """
      class ShallowImmutable {
        val shallowImmutable: Mutable = new Mutable
      }
      class Mutable {
        var mutable: Int = 0
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("ShallowImmutable") -> Utils.IsShallowImmutable, List("Mutable") -> Utils.IsMutable)) {
      """
      class ShallowImmutable {
        val immutable: Mutable = new Mutable
        class Mutable {
          var mutable: Int = 0
        }
      }
      """
    }
    TestUtils.expectMutability(Map(List("ShallowImmutable") -> Utils.IsShallowImmutable, List("Mutable") -> Utils.IsMutable)) {
      """
      class ShallowImmutable {
        class Mutable {
          var mutable: Int = 0
        }
        val immutable: Mutable = new Mutable
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("ShallowImmutable", "ShallowImmutableTwo") -> Utils.IsShallowImmutable, List("DefinitelyMutable") -> Utils.IsMutable)) {
      """
      class ShallowImmutable {
        val immutable: ShallowImmutableTwo = new ShallowImmutableTwo
        class ShallowImmutableTwo {
           val str: DefinitelyMutable = new DefinitelyMutable
        }
      }
      class DefinitelyMutable {
        var state: Int = 0
      }
      """
    }
    TestUtils.expectMutability(Map(List("ShallowImmutable", "ShallowImmutableTwo") -> Utils.IsShallowImmutable, List("DefinitelyMutable") -> Utils.IsMutable)) {
      """
      class DefinitelyMutable {
        var state: Int = 0
      }
      class ShallowImmutable {
        val immutable: ShallowImmutableTwo = new ShallowImmutableTwo
        class ShallowImmutableTwo {
           val str: DefinitelyMutable = new DefinitelyMutable
        }
      }
      """
    }
    TestUtils.expectMutability(Map(List("ShallowImmutable", "ShallowImmutableTwo") -> Utils.IsShallowImmutable, List("DefinitelyMutable") -> Utils.IsMutable)) {
      """
      class DefinitelyMutable {
        var state: Int = 0
      }
      class ShallowImmutable {
        val immutable: ShallowImmutableTwo = new ShallowImmutableTwo
      }
      class ShallowImmutableTwo {
        val str: DefinitelyMutable = new DefinitelyMutable
      }
      """
    }
  }

  it should testNr in {
    val expect = Map(List("A", "B") -> Utils.IsShallowImmutable, List("C") -> Utils.IsMutable, List("D") -> Utils.IsDeeplyImmutable)
    TestUtils.expectMutability(expect) {
      """
      class A {
        val foo: B = new B
        class B {
          val bar: C = new C
          class C {
            var baz: D = new D
            class D {
              val test: Int = 0
            }
          }
        }
      }
      """
    }
    TestUtils.expectMutability(expect) {
      """
      class A {
        val foo: B = new B
      }
      class B {
        val bar: C = new C
      }
      class C {
        var baz: D = new D
      }
      class D {
        val test: Int = 0
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Test") -> Utils.IsDeeplyImmutable)) {
      """
        class A {}
        class Test {
          val fn = new A
        }
      """
    }

    TestUtils.expectMutability(Map(List("Test") -> Utils.IsShallowImmutable, List("A") -> Utils.IsMutable)) {
      """
      class A {
        var foo: String = null
      }
      class Test {
        val fn = new A
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Test") -> Utils.IsDeeplyImmutable, List("Foo") -> Utils.IsConditionallyImmutable)) {
      """
      object Test {

        val f1 = Foo {
          println("hello from the `f1` instance")
          "this is the result of the block of code"
        }

        f1.print1

      }
      case class Foo[A, B](f: A => B) {
        println(f)
        def print1 {
          println("1")
        }
      }
      """
    }
  }
}
