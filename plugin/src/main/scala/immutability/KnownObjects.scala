package immutability

import scala.collection.mutable.ArrayBuffer

object KnownObjects {

  val NoType = "<notype>"

  // http://docs.scala-lang.org/tutorials/tour/unified-types.html
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
    "Serializable", // For case classes
    "Product", // For case classes and objects
    "Option",
    "Null",
    "Nothing",
    "trait Function1",
    "java.io.Serializable"
  )
  // Aliases for mutable in scala/package.scala
  val MutableAliases = Set(
    "Stringbuilder"
  )

  // All immutable
  val PreDefAliases = Set(
    "Map",
    "Set",
    "Function",
    "Function1",
    "String",
    "Pair", // ?
    "Triple"
  )

  val RootPackageAliases = Set(
    "TraversableOnce",
    "Traversable",
    "Iterable",
    "Seq",
    "IndexedSeq",
    "Iterator",
    "BufferedIterator",
    "+:",
    ":+"
  )

  // Aliases for scala.collection.immutable in scala/package.scala
  val ImmutablePackageAliases = Set(
    "List",
    "Nil",
    "::",
    "Stream",
    "#::",
    "Vector",
    "Range"
  )

  val NumericPackageAliases = Set(// TODO
    "BigDecimal",
    "BigInt",
    "Equiv",
    "Fractional",
    "Ordered",
    "Ordering",
    "PartiallyOrdered"
  )

  val ScalaUtilPackageAliases = Set(// TODO
    "Either",
    "Left",
    "Right"
  )

  val ExceptionPackageAliases = Set(
    "Throwable",
    "Exception",
    "Error",
    "RuntimeException",
    "NullPointerException",
    "ClassCastException",
    "IndexOutOfBoundsException",
    "ArrayIndexOutOfBoundsException",
    "StringIndexOutOfBoundsException",
    "UnsupportedOperationException",
    "IllegalArgumentException",
    "NoSuchElementException",
    "NumberFormatException",
    "AbstractMethodError",
    "InterruptedException"
  )

  val MutableTypes = Set(
    "trait App" // If it's scala App trait
  )

  val ImmutablePackages = Set(
    //    "scala.collection.immutable",
    //    "scala.collection.parallel.immutable"
  )

  val MutablePackages = Set(
    //    "scala.collection.mutable",
    //    "scala.collection.concurrent", // Mutable, concurrent data-structures such as TrieMap
    //    "scala.collection.parallel.mutable"
  )

  // TODO
  // Add java things

  val JavaClassesUsed = ArrayBuffer[String]()

  def getMutability(typeString: String): Immutability = {
    // TODO: Check for trait Immutable?
    val baseClass = extractBaseClass(typeString)
    if (typeString.contains("java")) {
      JavaClassesUsed += typeString
    }
    if (baseClass == NoType) {
      Immutable
    } else if (ImmutableTypes.contains(baseClass) || ImmutablePackageAliases.contains(baseClass) || PreDefAliases.contains(baseClass) || ExceptionPackageAliases.contains(baseClass) || NumericPackageAliases.contains(baseClass)) {
      Immutable
    } else if (isInImmutablePackage(baseClass)) {
      Immutable
    } else if (isInMutablePackage(baseClass) || MutableAliases.contains(baseClass)) {
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

  def extractBaseClass(s: String): String = {
    s.split("\\[")(0)
  }
}
