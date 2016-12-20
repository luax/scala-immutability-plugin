package specs.analysis

import helpers.Utils
import org.scalatest._
import utils.TestUtils

class GenericSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Foo", "Mutable") -> Utils.IsMutable, List("Bar") -> Utils.IsShallowImmutable)) {
      """
      class Foo[T] {
        var mutable: String = "mutable"
      }

      class Mutable {
        var mutable: String = "mutable"
      }

      class Bar {
        val foo = new Foo[Mutable]()
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Person", "Program") -> Utils.IsDeeplyImmutable)) {
      """
      case class Person(name: String)
      class Program {
        val list: List[Person] = List(Person("Person 1"), Person("Person 2"))
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Container") -> Utils.IsMutable, List("Program") -> Utils.IsShallowImmutable)) {
      """
      case class Container(var x: String)
      class Program {
        val list: List[Container] = List(Container("Person 1"), Container("Person 2"))
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(List("A"), Utils.IsMutable) {
      """
        import scala.collection.mutable.ArrayBuffer
        class A {
          val fruits = ArrayBuffer[String]()
          def foo = {
            fruits += "Apple"
            fruits += "Banana"
            fruits += "Orange"
          }
        }
      """
    }
  }
}
