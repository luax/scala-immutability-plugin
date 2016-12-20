package specs.analysis

import helpers.Utils
import org.scalatest.FlatSpec
import utils.TestUtils

class SpecialCasesSpec extends FlatSpec {

  it should "Serializable" in {
    TestUtils.expectMutability(Map(List("Serializable") -> Utils.IsDeeplyImmutable)) {
      """
      trait Serializable extends Any with java.io.Serializable
      """
    }
  }

}
