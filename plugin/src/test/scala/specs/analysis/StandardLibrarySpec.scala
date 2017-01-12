package specs.analysis

import helpers.Utils
import org.scalatest._
import utils.TestUtils

class StandardLibrarySpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
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

  it should testNr in {
    TestUtils.expectMutability(List("A"), Utils.IsDeeplyImmutable) {
      """
      class A {
        val fruits = List[String]()
      }
    """
    }
    TestUtils.expectMutability(List("A"), Utils.IsMutable) {
      """
      class A {
        val fruits = Seq[String]()
      }
    """
    }
  }
}
