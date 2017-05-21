package immutability

import helpers.Utils

object Assumptions {

  // http://docs.scala-lang.org/tutorials/tour/unified-types.html
  val ScalaAssumedImmutableTypes = Set(
    "scala.Any",
    "scala.AnyVal",
    "scala.Boolean",
    "scala.Byte",
    "scala.Char",
    "scala.Double",
    "scala.Float",
    "scala.Function",
    "scala.Function0",
    "scala.Function1",
    "scala.Function10",
    "scala.Function11",
    "scala.Function12",
    "scala.Function13",
    "scala.Function14",
    "scala.Function15",
    "scala.Function16",
    "scala.Function17",
    "scala.Function18",
    "scala.Function19",
    "scala.Function2",
    "scala.Function20",
    "scala.Function21",
    "scala.Function22",
    "scala.Function3",
    "scala.Function4",
    "scala.Function5",
    "scala.Function6",
    "scala.Function7",
    "scala.Function8",
    "scala.Function9",
    "scala.Int",
    "scala.Long",
    "scala.None",
    "scala.Nothing",
    "scala.Null",
    "scala.Option",
    "scala.Product",
    "scala.Serializable",
    "scala.Some",
    "scala.Tuple1",
    "scala.Tuple2",
    "scala.Tuple3",
    "scala.Tuple4",
    "scala.Tuple5",
    "scala.Tuple6",
    "scala.Tuple7",
    "scala.Tuple8",
    "scala.Tuple9",
    "scala.Tuple10",
    "scala.Tuple11",
    "scala.Tuple12",
    "scala.Tuple13",
    "scala.Tuple14",
    "scala.Tuple15",
    "scala.Tuple16",
    "scala.Tuple17",
    "scala.Tuple18",
    "scala.Tuple19",
    "scala.Tuple20",
    "scala.Tuple21",
    "scala.Tuple22",
    "scala.Unit"
  )

  val JavaAssumedImmutableTypes = Set(
    "java.io.File",
    "java.io.Serializable",
    "java.lang.CharSequence",
    "java.lang.Class",
    "java.lang.Cloneable",
    "java.lang.Comparable",
    "java.lang.Error",
    "java.lang.Exception",
    "java.lang.IllegalArgumentException",
    "java.lang.Iterable",
    "java.lang.Object",
    "java.lang.Process",
    "java.lang.Runnable",
    "java.lang.RuntimeException",
    "java.lang.String",
    "java.lang.Throwable",
    "java.math.BigInteger",
    "java.util.AbstractCollection",
    "java.util.AbstractSet",
    "java.util.Comparator",
    "java.util.Dictionary",
    "java.util.Enumeration",
    "java.util.Iterator",
    "java.util.List",
    "java.util.Map",
    "java.util.Random",
    "java.util.Set"
  )

  // These can be both Mutable and Immutable
  val ScalaMutableTypes = Set(
    "scala.collection.TraversableOnce",
    "scala.collection.Traversable",
    "scala.collection.Iterable",
    "scala.collection.Seq",
    "scala.collection.IndexedSeq",
    "scala.collection.Iterator",
    "scala.collection.BufferedIterator",
    "scala.collection.+:",
    "scala.collection.:+"
  )

  // Documented as immutable
  val ImmutablePackages = Set(
    "scala.collection.immutable",
    "scala.collection.parallel.immutable"
  )

  // Documented as mutable
  val MutablePackages = Set(
    "scala.collection.mutable",
    "scala.collection.concurrent", // Mutable, concurrent data-structures such as TrieMap
    "scala.collection.parallel.mutable"
  )

  var JavaClassesUsed: Set[String] = Set()
  var UnidentifiedTypes: Set[String] = Set()

  def getImmutabilityAssumption(typeString: String): Immutability = {
    val baseClass = extractBaseClass(typeString)
    if (typeString.contains("java")) {
      JavaClassesUsed += typeString
    }
    val immutability = getImmutabilityAssumption(baseClass, typeString)
    immutability match {
      case Immutable => {
        Utils.log(s"Assuming '${baseClass}' to be Immutable")
      }
      case MutabilityUnknown => {
        Utils.log(s"No idea about '${baseClass}' returning MutabilityUnknown (will be treated as Mutable, there was no cell completer for this class)")
      }
      case Mutable => {
        Utils.log(s"Assuming '${baseClass}' to be Mutable")
      }
      case _ =>
    }
    immutability
  }

  private def getImmutabilityAssumption(baseClass: String, typeString: String): Immutability = {
    if (ScalaAssumedImmutableTypes.contains(baseClass) || JavaAssumedImmutableTypes.contains(baseClass)) {
      Immutable
    } else {
      if (Utils.MakeAssumptionAboutTypes || Utils.isScalaTest) {
        if (isInImmutablePackage(baseClass)) {
          Immutable
        } else {
          UnidentifiedTypes += typeString
          MutabilityUnknown
        }
      } else {
        UnidentifiedTypes += typeString
        MutabilityUnknown
      }
    }
  }

  private def isInImmutablePackage(typeString: String): Boolean = {
    ImmutablePackages.exists(isInPackage(_, typeString))
  }

  private def isInMutablePackage(typeString: String): Boolean = {
    MutablePackages.exists(isInPackage(_, typeString))
  }

  private def isInPackage(pack: String, typeString: String): Boolean = {
    typeString.startsWith(pack)
  }

  def extractBaseClass(s: String): String = {
    s.split("\\[")(0)
  }
}

