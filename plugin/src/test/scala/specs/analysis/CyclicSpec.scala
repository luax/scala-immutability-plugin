package specs.analysis

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
    TestUtils.expectMutability(List("A", "B"), Utils.IsDeeplyImmutable) {
      """
      class A {
        val b: B = new B
      }
      class B {
        val a: A = new A
      }
      """
    }
    TestUtils.expectMutability(List("A", "B"), Utils.IsDeeplyImmutable) {
      """
      class B {
        val a: A = new A
      }
      class A {
        val b: B = new B
      }
      """
    }
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

  it should testNr in {
    TestUtils.expectMutability(Map(List("C", "B") -> Utils.IsShallowImmutable, List("A") -> Utils.IsMutable)) {
      """
      class C {
        val a: A = new A
        val B: B = new B
      }
      class A {
        var c: C = new C
      }
      class B {
        val c: C = new C
      }
      """
    }

    TestUtils.expectMutability(Map(List("C", "B") -> Utils.IsShallowImmutable, List("A") -> Utils.IsMutable)) {
      """
      class A {
        var c: C = new C
      }
      class C {
        val a: A = new A
        val B: B = new B
      }
      class B {
        val c: C = new C
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(List("A", "B", "C", "D"), Utils.IsDeeplyImmutable) {
      """
      class A {
        val b: B = new B
      }
      class B {
        val c: C = new C
      }
      class C {
        val d: D = new D
      }
      class D {
        val a: A = new A
      }
      """
    }
    TestUtils.expectMutability(List("A", "B", "C", "D"), Utils.IsDeeplyImmutable) {
      """
      class D extends C {
        val a: A = new A
      }
      class C extends B {
        val d: D = new D
      }
      class B extends A {
        val c: C = new C
      }
      class A {
        val b: B = new B
      }
      """
    }
  }
}
