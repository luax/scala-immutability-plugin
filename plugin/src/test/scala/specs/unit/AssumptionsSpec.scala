package specs.unit

import immutability.{Assumptions, Immutable, MutabilityUnknown, Mutable}
import org.scalatest._

class AssumptionsSpec extends FlatSpec {

  it should "return immutable" in {
    assert(Assumptions.getImmutabilityAssumption("scala.collection.immutable.HashMap[Int,String]") == Immutable)
    assert(Assumptions.getImmutabilityAssumption("scala.collection.immutable.HashMap[String]") == Immutable)
    assert(Assumptions.getImmutabilityAssumption("scala.collection.immutable.List") == Immutable)
    assert(Assumptions.getImmutabilityAssumption("scala.String") == Immutable)
    assert(Assumptions.getImmutabilityAssumption("scala.Int") == Immutable)
    assert(Assumptions.getImmutabilityAssumption("<notype>") == MutabilityUnknown)
  }

  it should "return mutable" in {
    assert(Assumptions.getImmutabilityAssumption("scala.collection.mutable.ArrayBuffer[String]") == Mutable)
  }

  it should "return unknown" in {
    assert(Assumptions.getImmutabilityAssumption("Foo bar") == MutabilityUnknown)
  }

  it should "extract base class" in {
    assert(Assumptions.extractBaseClass("scala.collection.mutable.ArrayBuffer[String]") == "scala.collection.mutable.ArrayBuffer")
    assert(Assumptions.extractBaseClass("scala.collection.mutable.ArrayBuffer[String, Int]") == "scala.collection.mutable.ArrayBuffer")
    assert(Assumptions.extractBaseClass("scala.collection.mutable.ArrayBuffer[String, Int, Long]") == "scala.collection.mutable.ArrayBuffer")
    assert(Assumptions.extractBaseClass("List[String]") == "List")
    assert(Assumptions.extractBaseClass("List[String, Double]") == "List")
    assert(Assumptions.extractBaseClass("List") == "List")
    assert(Assumptions.extractBaseClass("scala.collection.mutable.ArrayBuffer") == "scala.collection.mutable.ArrayBuffer")
  }
}
