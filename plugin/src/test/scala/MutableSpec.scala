import helpers.Utils
import org.scalatest._

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
    TestUtils.expectMutability(Map(List("D", "C") -> Utils.IsMutable, List("A", "B") -> Utils.IsDeeplyImmutable)) {
      """
      class A {
        val foo: String = "immutable"
      }

      class B extends A {
        val bar: String = "immutable"
      }

      class C extends B {
        var baz: String = "mutate me"
      }

      class D extends C {
        // val test: String = "immutable"
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("A", "B", "C", "D") -> Utils.IsMutable, List("E") -> Utils.IsDeeplyImmutable)) {
      """
      class A {
        var foo: String = "mutate me"
      }

      class B extends A {
        val bar: String = "immutable"
      }

      class C extends B {
        val baz: String = "immutable"
      }

      class D extends C {
        val test: String = "immutable"
      }

      class E()
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
    TestUtils.expectMutability("A", Utils.IsMutable) {
      """
      class A(var foo: String)
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability("A", Utils.IsMutable) {
      """
      class B {
        val foo: String = "immutable"
      }

      class A {
        var foo: B = new B
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(List("A", "B", "C", "D"), Utils.IsMutable) {
      """
      class D extends C {
        val test: String = "immutable";
      }

      class C extends B {
        val baz: String = "immutable";
      }

      class B extends A {
        val bar: String = "immutable";
      }

      class A {
        var foo: String = "mutate me";
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("C", "D") -> Utils.IsMutable, List("A", "B") -> Utils.IsDeeplyImmutable)) {
      """
      class D extends C {
        val test: String = "immutable";
      }

      class C extends B {
        var baz: String = "mutable";
      }

      class B extends A {
        val bar: String = "immutable";
      }

      class A {
        val foo: String = "mutate me";
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Immutable") -> Utils.IsDeeplyImmutable, List("Mutable", "A") -> Utils.IsMutable)) {
      """
      trait Mutable {
        var mutable: Int = 0
      }
      trait Immutable {
        val immutable: Int = 0
      }
      class A extends Mutable with Immutable {
        val foo: String = "immutable"
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Immutable", "Foo") -> Utils.IsDeeplyImmutable, List("A") -> Utils.IsMutable)) {
      """
      trait Foo {
        val immutablee: Int = 0
      }
      trait Immutable {
        val immutable: Int = 0
      }
      class A extends Foo with Immutable {
        var foo: String = "immutable"
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Immutable") -> Utils.IsDeeplyImmutable, List("Mutable") -> Utils.IsMutable)) {
      """
      class Immutable {
        val immutable: Mutable = new Mutable
        class Mutable {
          var mutable: Int = 0
        }
      }
      """
    }
  }
}
