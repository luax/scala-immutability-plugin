import org.scalatest._
import helpers.Utils

class MutableSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  "Mutable" should testNr in {
    TestUtils.expectMutability("A", Utils.IsMutable) {
      """
      class A {
        var foo: String = "mutate me";
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability("D", Utils.IsMutable) {
      """
      class A {
        var foo: String = "mutate me";
      }

      class B extends A {
        val bar: String = "immutable";
      }

      class C extends B {
        var baz: String = "mutate me";
      }

      class D extends C {
        // val test: String = "immutable";
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability("D", Utils.IsMutable) {
      """
      class A {
        var foo: String = "mutate me";
      }

      class B extends A {
        val bar: String = "immutable";
      }

      class C extends B {
        val baz: String = "mutate me";
      }

      class D extends C {
        val test: String = "immutable";
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability("C", Utils.IsMutable) {
      """
      class A {
        var foo: String = "mutate me";
      }

      class B extends A {
        val bar: String = "immutable";
      }

      class C extends B {
        val baz: String = "mutate me";
      }

      class D extends C {
        val test: String = "immutable";
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability("A", Utils.IsMutable) {
      """
      class A {
        var foo: String = "mutate me";
      }

      class B extends A {
        val bar: String = "immutable";
      }

      class C extends B {
        val baz: String = "mutate me";
      }

      class D extends C {
        val test: String = "immutable";
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability("A", Utils.IsMutable) {
      """
      class A {
        var foo: String = "mutate me";
        val bar: String = "immutable"
        val baz: String = "immutable"
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability("A", Utils.IsMutable) {
      """
      class A(var foo: String) {
      }
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
}
