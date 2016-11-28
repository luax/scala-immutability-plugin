import helpers.Utils
import org.scalatest._

class DevelopmentSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Immutable") -> Utils.IsDeeplyImmutable, List("Mutable", "A") -> Utils.IsMutable)) {
      """
      class Mutable {
        var mutable: Int = 0
      }
      class Immutable {
        val immutable: Int = 0
      }
      class A extends Mutable {
        val foo: String = "immutable"
      }
      """
    }
  }

}
