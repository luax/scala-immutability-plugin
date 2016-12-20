package specs.analysis

import helpers.Utils
import org.scalatest._
import utils.TestUtils

class NestedClassesSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Animal", "Foo") -> Utils.IsDeeplyImmutable, List("Bar", "Cool") -> Utils.IsShallowImmutable)) {
      """
      class Animal () {
        def sound = {
          0
        }

      }
      class Foo () {
        def biophony[T <: Animal](things: Seq[T]) = things map (_.sound)
      }

      class Cool[K <: Animal]() {
        val bars: List[Bar[K]] = List()
        val barsTest = List(Seq(new Animal()))
      }

      class Bar[T <: Animal](things: Seq[T]) {

      }
      """
    }

  }

}
