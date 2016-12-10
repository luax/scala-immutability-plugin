package specs.unit

import immutability.{Immutable, KnownObjects, MutabilityUnknown, Mutable}
import org.scalatest._

class KnownObjectsSpec extends FlatSpec {

  it should "return immutable" in {
    assert(KnownObjects.getMutability("scala.collection.immutable.HashMap[Int,String]") == Immutable)
    assert(KnownObjects.getMutability("scala.collection.immutable.HashMap[String]") == Immutable)
    assert(KnownObjects.getMutability("String") == Immutable)
    assert(KnownObjects.getMutability("List") == Immutable)
    assert(KnownObjects.getMutability("Int") == Immutable)
    assert(KnownObjects.getMutability("<notype>") == Immutable) // TODO: Unknown?
  }

  it should "return mutable" in {
    assert(KnownObjects.getMutability("scala.collection.mutable.ArrayBuffer[String]") == Mutable)
  }

  it should "return unknown" in {
    assert(KnownObjects.getMutability("Foo bar") == MutabilityUnknown)
  }
}
