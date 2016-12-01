import helpers.Utils
import org.scalatest._

class DevelopmentSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("A") -> Utils.IsDeeplyImmutable)) {
      """
      class A  {
        lazy val foo = new String("Bar")
      }
      """
    }
  }

}
