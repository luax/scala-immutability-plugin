package specs.analysis

import helpers.Utils
import org.scalatest._
import utils.TestUtils

class CaseClasses extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  it should testNr in {
    TestUtils.expectMutability(List("Mutable"), Utils.IsMutable) {
      """
        case class Mutable() {
          var bar: String = "mutable"
          def foo(): Unit = {}
        }
      """
    }
    TestUtils.expectMutability(List("Mutable"), Utils.IsMutable) {
      """
        case class Mutable(var foo: String) {
          val bar: String = "mutable"
        }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(List("Foo", "Test"), Utils.IsMutable) {
      """
        class Foo {
          var mutable: String = "mutable"
        }
        case class Test() extends Foo
      """
    }
    TestUtils.expectMutability(List("Foo", "Test"), Utils.IsMutable) {
      """
        case class Test() extends Foo
        class Foo {
          var mutable: String = "mutable"
        }
      """
    }
    TestUtils.expectMutability(Map(List("Test") -> Utils.IsMutable, List("Foo") -> Utils.IsDeeplyImmutable)) {
      """
        class Foo {
          val a: String = "foo"
        }
        case class Test(var b: Int) extends Foo
      """
    }
    TestUtils.expectMutability(Map(List("Test") -> Utils.IsMutable, List("Foo") -> Utils.IsDeeplyImmutable)) {
      """
        case class Test(var b: Int) extends Foo
        class Foo {
          val a: String = "foo"
        }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(List("Immutable"), Utils.IsDeeplyImmutable) {
      """
        case class Immutable() {
          val bar: String = "immutable"
          def foo(): Unit = {}
        }
      """
    }
    TestUtils.expectMutability(List("Immutable"), Utils.IsDeeplyImmutable) {
      """
        case class Immutable(foo: String) {
          val bar: String = "immutable"
        }
      """
    }
  }


  it should testNr in {
    TestUtils.expectMutability(List("Foo", "Test", "Bar"), Utils.IsMutable) {
      """
        class Foo extends Bar {
          val a: String = "b"
        }
        case class Test() extends Foo
        class Bar {
          var mutable: String = "mutable"
        }
      """
    }
  }


  it should testNr in {
    TestUtils.expectMutability(Map(List("Tree", "Node") -> Utils.IsDeeplyImmutable)) {
      """
      sealed abstract class Tree
      case class Node(left: Tree, right: Tree) extends Tree
      """
    }

    TestUtils.expectMutability(Map(List("Tree", "Node") -> Utils.IsDeeplyImmutable, List("Leaf") -> Utils.IsMutable)) {
      """
      sealed abstract class Tree
      case class Node(left: Tree, right: Tree) extends Tree
      case class Leaf[A](var value: A) extends Tree
      """
    }

    TestUtils.expectMutability(Map(List("Tree", "Node", "EmptyLeaf") -> Utils.IsDeeplyImmutable, List("Leaf") -> Utils.IsMutable)) {
      """
      sealed abstract class Tree
      case class Node(left: Tree, right: Tree) extends Tree
      case class Leaf[A](var value: A) extends Tree
      case object EmptyLeaf extends Tree
      """
    }

    TestUtils.expectMutability(Map(List("Tree", "Node", "EmptyLeaf") -> Utils.IsDeeplyImmutable, List("Leaf", "Foo") -> Utils.IsMutable)) {
      """
      sealed abstract class Tree
      case class Node(left: Tree, right: Tree) extends Tree
      case class Leaf[A](var value: A) extends Tree
      case object EmptyLeaf extends Tree
      case class Foo(var foo: String) extends Tree
      """
    }

    TestUtils.expectMutability(Map(List("EmptyLeaf") -> Utils.IsDeeplyImmutable, List("Tree", "Node", "Leaf", "Foo") -> Utils.IsMutable)) {
      """
      sealed class Tree {
        var mutable: String = "foo"
      }
      case class Node(left: Tree, right: Tree) extends Tree
      case class Leaf[A](var value: A) extends Tree
      case object EmptyLeaf
      case class Foo(var foo: String)
      """
    }
  }

}
