package immutability

import helpers.Utils

object Assumptions {

  // http://docs.scala-lang.org/tutorials/tour/unified-types.html
  val ScalaAssumedImmutableTypes = Set(
    "scala.Any",
    "scala.AnyVal",
    "scala.String",
    "scala.Object",
    "scala.Double",
    "scala.Float",
    "scala.Long",
    "scala.Int",
    "scala.Short",
    "scala.Byte",
    "scala.Char",
    "scala.Boolean",
    "scala.Unit",
    "scala.AnyRef", // (java.lang.object)
    "scala.ScalaObject",
    "scala.Null",
    "scala.Nothing",
    "scala.Product", // For case classes and objects
    "scala.Option",
    "scala.None",
    "scala.Some",
    "scala.Null",
    "scala.Nothing",
    "scala.Function1",
    "scala.Serializable"
  )

  val JavaAssumedImmutableTypes = Set(
    "java.io.Serializable", // For case classes
    "java.lang.Comparable",
    "java.lang.Object",
    "java.lang.Boolean",
    "java.lang.Byte",
    "java.lang.CharSequence",
    "java.lang.Character",
    "java.lang.Class",
    "java.lang.Cloneable",
    "java.lang.Comparable",
    "java.lang.Double",
    "java.lang.Error",
    "java.lang.Exception",
    "java.lang.Float",
    "java.lang.Integer",
    "java.lang.Iterable",
    "java.lang.Long",
    "java.lang.Math",
    "java.lang.Object",
    "java.lang.Runnable",
    "java.lang.Short",
    "java.lang.StrictMath",
    "java.lang.String",
    "java.lang.Void",
    "java.util.List",
    "java.util.Map",
    "java.util.Set",
    // Used by
    "java.io.File",
    "java.math.BigInteger",
    "java.util.Random",
    "java.util.Iterator",
    "java.util.Comparator",
    "java.util.AbstractCollection",
    "java.util.AbstractSet",
    "java.lang.Process",
    "java.util.Dictionary",
    "java.util.Enumeration",
    // Below found in scala/package.scala
    "java.lang.Throwable",
    "java.lang.Exception",
    "java.lang.Error",
    "java.lang.RuntimeException",
    "java.lang.NullPointerException",
    "java.lang.ClassCastException",
    "java.lang.IndexOutOfBoundsException",
    "java.lang.ArrayIndexOutOfBoundsException",
    "java.lang.StringIndexOutOfBoundsException",
    "java.lang.UnsupportedOperationException",
    "java.lang.IllegalArgumentException",
    "java.util.NoSuchElementException",
    "java.lang.NumberFormatException",
    "java.lang.AbstractMethodError",
    "java.lang.InterruptedException"
  )

  val ScalaImmutableTypes = Set(
    "scala.math.BigDecimal",
    "scala.math.BigInt",
    "scala.math.Equiv",
    "scala.math.Fractional",
    "scala.math.Integral",
    "scala.math.Numeric",
    "scala.math.Ordered",
    "scala.math.Ordering",
    "scala.math.PartialOrdering",
    "scala.math.PartiallyOrdered",
    // Unknown
    "scala.util.Either",
    "scala.util.Left",
    "scala.util.Right",
    // Root package (filtered)
    //    "scala.AnyVal",
    //    "scala.AnyValCompanion",
    //    "scala.App",
    //    "scala.Array",
    //    "scala.Boolean",
    //    "scala.Byte",
    //    "scala.Char",
    //    "scala.Cloneable",
    //    "scala.Console",
    //    "scala.DelayedInit",
    //    "scala.Double",
    //    "scala.Dynamic",
    //    "scala.Enumeration",
    //    "scala.Equals",
    //    "scala.Float",
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
    //    "scala.Immutable",
    //    "scala.Int",
    //    "scala.Long",
    //    "scala.MatchError",
    //    "scala.Mutable",
    //    "scala.NotImplementedError",
    //    "scala.NotNull",
    //    "scala.Option",
    //    "scala.PartialFunction",
    //    "scala.Predef",
    "scala.Product",
    "scala.Product1",
    "scala.Product10",
    "scala.Product11",
    "scala.Product12",
    "scala.Product13",
    "scala.Product14",
    "scala.Product15",
    "scala.Product16",
    "scala.Product17",
    "scala.Product18",
    "scala.Product19",
    "scala.Product2",
    "scala.Product20",
    "scala.Product21",
    "scala.Product22",
    "scala.Product3",
    "scala.Product4",
    "scala.Product5",
    "scala.Product6",
    "scala.Product7",
    "scala.Product8",
    "scala.Product9",
    //    "scala.Proxy",
    //    "scala.Responder",
    //    "scala.SerialVersionUID",
    //    "scala.Serializable",
    //    "scala.Short",
    //    "scala.Specializable",
    //    "scala.StringContext",
    //    "scala.Symbol",
    "scala.Tuple1",
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
    "scala.Tuple2",
    "scala.Tuple20",
    "scala.Tuple21",
    "scala.Tuple22",
    "scala.Tuple3",
    "scala.Tuple4",
    "scala.Tuple5",
    "scala.Tuple6",
    "scala.Tuple7",
    "scala.Tuple8",
    "scala.Tuple9"
    //    "scala.UninitializedError",
    //    "scala.UninitializedFieldError",
    //    "scala.Unit"
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
        if (ScalaImmutableTypes.contains(baseClass) || isInImmutablePackage(baseClass)) {
          Immutable
        } else if (ScalaMutableTypes(baseClass) || isInMutablePackage(baseClass)) {
          Mutable
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

