package specs.analysis

import helpers.Utils
import org.scalatest._
import utils.TestUtils

class PrivateVarSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  if (Utils.AllowPrivateVar) {
    it should testNr in {
      TestUtils.expectMutability(Map(List("A") -> Utils.IsDeeplyImmutable)) {
        """
        class A {
          private var a: Int = 1
        }
        """
      }
    }


    it should testNr in {
      TestUtils.expectMutability(Map(List("B") -> Utils.IsDeeplyImmutable)) {
        """
        class B {
          private[this] var b: Int = 2
        }
        """
      }
    }


    it should testNr in {
      TestUtils.expectMutability(Map(List("C") -> Utils.IsDeeplyImmutable)) {
        """
        class C {
          val valField: String = "Immutable"
          private var a: Int = 1
        }
        """
      }
    }
  }

}
