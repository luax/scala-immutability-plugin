package immutability

import helpers.Utils

object KnownObjects {

  val NoType = "<notype>"

  val ImmutableTypes = Set(
    "Any", // TODO
    "AnyVal", // TODO
    "String", // String is an alias for java.lang.String.
    "Object", // TODO: Known immutable?
    "Double",
    "Float",
    "Long",
    "Int",
    "Short",
    "Byte",
    "Char",
    "Boolean",
    "Unit",
    "AnyRef", //(java.lang.object)
    "ScalaObject",
    "Null",
    "Nothing",
    "List" //  List is an alias for scala.collection.immutable.List.
  )

  val ConditionallyImmutable = Set( // TODO:
    "List",
    "Seq"
  )

  val ImmutablePackages = List(
    "scala.collection.immutable",
    "scala.collection.parallel.immutable"
  )

  val MutablePackages = List(
    "scala.collection.mutable",
    "scala.collection.concurrent", // Mutable, concurrent data-structures such as TrieMap
    "scala.collection.parallel.mutable"
  )

  def getMutability(typeString: String): Immutability = {
    if (typeString == NoType) {
      Immutable
    } else if (ImmutableTypes.contains(typeString)) {
      Immutable
    } else if (isInImmutablePackage(typeString)) {
      Immutable
    } else if (isInMutablePackage(typeString)) {
      Mutable
    } else {
      MutabilityUnknown
    }
  }

  def isInImmutablePackage(typeString: String): Boolean = {
    ImmutablePackages.exists(isInPackage(_, typeString))
  }

  def isInMutablePackage(typeString: String): Boolean = {
    MutablePackages.exists(isInPackage(_, typeString))
  }

  def isInPackage(pack: String, typeString: String): Boolean = {
    typeString.startsWith(pack)
  }
}
