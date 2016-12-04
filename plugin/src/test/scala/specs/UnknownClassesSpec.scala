package specs

import helpers.Utils
import org.scalatest._
import utils.TestUtils

class UnknownClassesSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("A") -> Utils.IsDeeplyImmutable)) { // TODO: Unknown
      """
      import secret._

      class A {
        val str: secret.SecretClass = new secret.SecretClass
      }
      """
    }
  }

}
