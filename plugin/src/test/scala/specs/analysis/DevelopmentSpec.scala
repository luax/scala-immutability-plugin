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

  TestUtils.expectMutability(Map(List("Tree") -> Utils.IsDeeplyImmutable)) {
    // TODO case object
    """
      sealed abstract class Tree
      case class Node(left: Tree, right: Tree) extends Tree
      case class Leaf[A](value: A) extends Tree
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
