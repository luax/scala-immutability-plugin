package specs.analysis

import org.scalatest._

class DevelopmentSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  //  it should testNr in {
  //    TestUtils.expectMutability(Map(List("Implementation") -> Utils.IsShallowImmutable, List("Mutable") -> Utils.IsMutable)) {
  //      """
  //      type T = Mutable
  //
  //      class Implementation {
  //        val t: T = new T
  //      }
  //
  //      class Mutable {
  //        var foo: String = "mutable"
  //      }
  //      """
  //    }
  //  }
  //
  //  class Implementation {
  //    val t: String = "foo"
  //  }
  //
  //  var f = Seq("1", "2")
  //
  //
  //  it should testNr in {
  //    TestUtils.expectMutability(Map(List("Implementation") -> Utils.IsDeeplyImmutable)) {
  //      """
  //      class Implementation {
  //        val t =  Seq("1", "2", "3")
  //        // val f: StringBuilder = null
  //      }
  //      """
  //    }
  //  }

  //
  //  it should testNr in {
  //    TestUtils.expectMutability(Map(List("Implementation") -> Utils.IsDeeplyImmutable)) {
  //      """
  //      trait Implementation {
  //        var a =  Seq("1", "2", "3")
  //      }
  //      """
  //    }
  //  }
}
