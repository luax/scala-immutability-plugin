package specs.analysis

import helpers.Utils
import org.scalatest._
import utils.TestUtils

class TypeArgumentsSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

  class Foo[T] {

  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Foo") -> Utils.IsShallowImmutable)) {
      """
        class Foo[T](val a: T)
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("T") -> Utils.IsDeeplyImmutable, List("Foo") -> Utils.IsShallowImmutable)) {
      """
        class T(val immutable: String)
        class Foo[T](val a: T)
      """
    }
  }

  it should testNr in {
    TestUtils.expectMutability(Map(List("Foo") -> Utils.IsMutable)) {
      """
        class Foo[T](var a: T)
      """
    }
  }
}
