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

  // http://stackoverflow.com/questions/36525804/scala-case-class-extending-product-with-serializable
  TestUtils.expectMutability(Map(List("Tree", "Node", "EmptyLeaf") -> Utils.IsDeeplyImmutable, List("Foo", "Leaf") -> Utils.IsMutable)) {
    // TODO case object
    """
      sealed abstract class Tree
      case class Node(left: Tree, right: Tree) extends Tree
      case class Leaf[A](var value: A) extends Tree // CONDITIONALLY
      case object EmptyLeaf extends Tree
      case class Foo(var bajs: String) extends Tree
    """
  }

  //  * Conditionally immutable means that the state of the instance of the respective class
  // * cannot be mutated, but objects referenced by it can be mutated (so called
  //   * immutable collections are typically rated as "conditionally immutable")

  // List[String] --> Immutable
  // List[Object] --> ConditionallyImmutable


  // List[String] --> Immutable
}
