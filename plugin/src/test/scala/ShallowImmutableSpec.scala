import helpers.Utils
import org.scalatest._

class ShallowImmutableSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  "Shallow immutable" should testNr in {
    TestUtils.expectMutability("A", Utils.IsShallowImmutable) {
      """
      class B {
        var foo: String = "mutable"
      }

      class A {
        val bar: String = "immutable"
        val foo: B = new B
      }
      """
    }
  }
}
