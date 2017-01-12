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

  it should "Ordered" in {
    TestUtils.expectMutability(Map(List("Ordered") -> Utils.IsConditionallyImmutable)) {
      """
      trait Ordered[A] extends Any with java.lang.Comparable[A] {
      }
      """
    }
  }

  //
  //    scala.collection.immutable.Vector
  //
  //    private[immutable] trait VectorPointer[T] {
  //      private[immutable] var depth: Int = _
  //    }

}
