package specs

import helpers.Utils
import org.scalatest._
import utils.TestUtils

class ShallowImmutableSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Immutable") -> Utils.IsShallowImmutable, List("Mutable") -> Utils.IsMutable)) {
      """
      class Immutable {
        val immutable: Mutable = new Mutable
        class Mutable {
          var mutable: Int = 0
        }
      }
      """
    }
  }


  it should testNr in {
    TestUtils.expectMutability(Map(List("ShallowImmutable") -> Utils.IsShallowImmutable, List("Mutable") -> Utils.IsMutable)) {
      """
      class Mutable {
        var mutable: Int = 0
      }

      class ShallowImmutable {
        val shallowImmutable: Mutable = new Mutable
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("ShallowImmutable") -> Utils.IsShallowImmutable, List("Mutable") -> Utils.IsMutable)) {
      """
      class ShallowImmutable {
        val immutable: Mutable = new Mutable
        class Mutable {
          var mutable: Int = 0
        }
      }
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("ShallowImmutable", "ShallowImmutableTwo") -> Utils.IsShallowImmutable, List("DefinitelyMutable") -> Utils.IsMutable)) {
      """
      class ShallowImmutable {
        val immutable: ShallowImmutableTwo = new ShallowImmutableTwo
        class ShallowImmutableTwo {
           val str: DefinitelyMutable = new DefinitelyMutable
        }
      }
      class DefinitelyMutable {
        var state: Int = 0
      }
      """
    }
  }
}
