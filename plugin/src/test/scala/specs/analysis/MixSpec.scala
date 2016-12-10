package specs.analysis

import helpers.Utils
import org.scalatest._
import utils.TestUtils

class MixSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("A") -> Utils.IsMutable, List("B") -> Utils.IsDeeplyImmutable)) {
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
