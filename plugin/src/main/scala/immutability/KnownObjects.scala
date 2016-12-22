package immutability

import scala.collection.mutable.ArrayBuffer

object KnownObjects {

  val NoType = "<notype>"

  // http://docs.scala-lang.org/tutorials/tour/unified-types.html

  val ScalaAssumedImmutableTypes = Set(
    "scala.Any", // TODO
    "scala.AnyVal", // TODO
    "scala.String", // String is an alias for java.lang.String.
    "java.lang.String",
    "scala.Object", // TODO: Known immutable?
    "scala.Double",
    "scala.Float",
    "scala.Long",
    "scala.Int",
    "scala.Short",
    "scala.Byte",
    "scala.Char",
    "scala.Boolean",
    "scala.Unit",
    "scala.AnyRef", //(java.lang.object)
    "java.lang.Object",
    "scala.ScalaObject",
    "scala.Null",
    "scala.Nothing",
    //"Serializable", // For case classes
    "scala.Product", // For case classes and objects
    "scala.Option",
    "scala.Null",
    "scala.Nothing",
    // "trait Function1",
    "scala.Function1",
    "scala.Serializable"
  )

  val ImmutableTypes = Set(
    "java.io.Serializable",
    "java.lang.Comparable",

    // Copy pasted"
    "java.beans.MethodDescriptor",
    "java.beans.PropertyDescriptor",
    "java.beans.SimpleBeanInfo",
    "java.io.BufferedReader",
    "java.io.Closeable",
    "java.io.DeleteOnExitHook",
    "java.io.File",
    "java.io.Flushable",
    "java.io.InputStream",
    "java.io.OutputStream",
    "java.io.PipedInputStream",
    "java.io.PipedOutputStream",
    "java.io.PrintStream",
    "java.io.PrintWriter",
    "java.io.Serializable",
    "java.lang.ApplicationShutdownHooks",
    "java.lang.Boolean",
    "java.lang.Byte",
    "java.lang.CharSequence",
    "java.lang.Character",
    "java.lang.CharacterData00",
    "java.lang.CharacterData01",
    "java.lang.CharacterData02",
    "java.lang.CharacterData0E",
    "java.lang.CharacterDataPrivateUse",
    "java.lang.CharacterDataUndefined",
    "java.lang.Class",
    "java.lang.ClassLoaderHelper",
    "java.lang.Cloneable",
    "java.lang.Comparable",
    "java.lang.Compiler",
    "java.lang.Double",
    "java.lang.Error",
    "java.lang.Exception",
    "java.lang.Float",
    "java.lang.IllegalArgumentException",
    "java.lang.InheritableThreadLocal",
    "java.lang.Integer",
    "java.lang.Iterable",
    "java.lang.Long",
    "java.lang.Math",
    "java.lang.Object",
    "java.lang.Process",
    "java.lang.ProcessBuilder",
    "java.lang.ProcessEnvironment",
    "java.lang.ProcessImpl",
    "java.lang.Runnable",
    "java.lang.Runtime",
    "java.lang.RuntimeException",
    "java.lang.RuntimePermission",
    "java.lang.Short",
    "java.lang.StrictMath",
    "java.lang.String",
    "java.lang.StringBuffer",
    "java.lang.StringBuilder",
    "java.lang.StringCoding",
    "java.lang.System",
    "java.lang.Thread",
    "java.lang.Thread.UncaughtExceptionHandler",
    "java.lang.ThreadLocal",
    "java.lang.Throwable",
    "java.lang.Void",
    "java.lang.annotation.ElementType",
    "java.lang.annotation.RetentionPolicy",
    "java.lang.invoke.InvokeDynamic",
    "java.lang.invoke.MethodHandleNatives",
    "java.lang.invoke.MethodHandleProxies",
    "java.lang.invoke.MethodHandleStatics",
    "java.lang.invoke.MethodHandles",
    "java.lang.invoke.SimpleMethodHandle",
    "java.lang.management.ManagementFactory",
    "java.lang.management.ManagementPermission",
    "java.lang.management.MemoryType",
    "java.lang.ref.PhantomReference",
    "java.lang.ref.ReferenceQueue",
    "java.lang.ref.SoftReference",
    "java.lang.ref.WeakReference",
    "java.lang.reflect.Array",
    "java.lang.reflect.Method",
    "java.lang.reflect.ReflectPermission",
    "java.math.BigDecimal",
    "java.math.BigInteger",
    "java.math.MathContext",
    "java.math.RoundingMode",
    "java.net.FactoryURLClassLoader",
    "java.net.IDN",
    "java.net.Inet4Address",
    "java.net.NetPermission",
    "java.net.StandardProtocolFamily",
    "java.net.StandardSocketOptions",
    "java.net.URL",
    "java.nio.Bits",
    "java.nio.channels.Channels",
    "java.nio.charset.Charset",
    "java.nio.charset.CoderResult",
    "java.nio.charset.StandardCharsets",
    "java.nio.file.AccessMode",
    "java.nio.file.CopyMoveHelper",
    "java.nio.file.DirectoryIteratorException",
    "java.nio.file.FileSystems",
    "java.nio.file.FileVisitOption",
    "java.nio.file.FileVisitResult",
    "java.nio.file.Files",
    "java.nio.file.LinkOption",
    "java.nio.file.LinkPermission",
    "java.nio.file.Paths",
    "java.nio.file.StandardCopyOption",
    "java.nio.file.StandardOpenOption",
    "java.nio.file.StandardWatchEventKinds",
    "java.nio.file.TempFileHelper",
    "java.nio.file.attribute.AclEntryFlag",
    "java.nio.file.attribute.AclEntryPermission",
    "java.nio.file.attribute.AclEntryType",
    "java.nio.file.attribute.PosixFilePermission",
    "java.nio.file.attribute.PosixFilePermissions",
    "java.rmi.Naming",
    "java.rmi.registry.LocateRegistry",
    "java.rmi.server.ObjID",
    "java.rmi.server.RMIClassLoader",
    "java.rmi.server.UID",
    "java.security.AccessController",
    "java.security.AllPermission",
    "java.security.CryptoPrimitive",
    "java.security.SecurityPermission",
    "java.security.cert.CRLReason",
    "java.security.cert.CertPathHelperImpl",
    "java.security.cert.PKIXReason",
    "java.sql.ClientInfoStatus",
    "java.sql.DriverManager",
    "java.sql.PseudoColumnUsage",
    "java.sql.RowIdLifetime",
    "java.sql.SQLPermission",
    "java.sql.Types",
    "java.text.CollationRules",
    "java.text.Normalizer",
    "java.util.AbstractCollection",
    "java.util.AbstractList",
    "java.util.AbstractMap",
    "java.util.AbstractSet",
    "java.util.Arrays",
    "java.util.Collection",
    "java.util.Collections",
    "java.util.Comparator",
    "java.util.Dictionary",
    "java.util.DualPivotQuicksort",
    "java.util.Enumeration",
    "java.util.FormattableFlags",
    "java.util.Iterator",
    "java.util.List",
    "java.util.LocaleISOData",
    "java.util.Map",
    "java.util.Objects",
    "java.util.Properties",
    "java.util.Random",
    "java.util.Set",
    "java.util.UUID",
    "java.util.WeakHashMap",
    "java.util.concurrent.Callable",
    "java.util.concurrent.ConcurrentLinkedQueue",
    "java.util.concurrent.ConcurrentMap",
    "java.util.concurrent.Executor",
    "java.util.concurrent.ExecutorService",
    "java.util.concurrent.Executors",
    "java.util.concurrent.ThreadFactory",
    "java.util.concurrent.ThreadPoolExecutor",
    "java.util.concurrent.TimeUnit",
    "java.util.concurrent.atomic.AtomicInteger",
    "java.util.concurrent.atomic.AtomicLong",
    "java.util.concurrent.atomic.AtomicReferenceFieldUpdater",
    "java.util.concurrent.locks.AbstractQueuedSynchronizer",
    "java.util.concurrent.locks.LockSupport",
    "java.util.concurrent.locks.ReentrantReadWriteLock",
    "java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock",
    "java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock",
    "java.util.jar.Attributes.Name",
    "java.util.logging.LoggingPermission",
    "java.util.logging.LoggingProxyImpl",
    "java.util.regex.ASCII",
    "java.util.regex.Matcher",
    "java.util.regex.Pattern",
    "java.util.zip.ZipConstants64"
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
    "scala.collection.immutable",
    "scala.collection.parallel.immutable"
  )

  val MutablePackages = Set(
    "scala.collection.mutable",
    "scala.collection.concurrent", // Mutable, concurrent data-structures such as TrieMap
    "scala.collection.parallel.mutable"
  )

  // TODO
  // Add java things
  var JavaClassesUsed:Set[String] = Set()

  def getMutability(typeString: String): Immutability = {
    // TODO: Check for trait Immutable?
    val baseClass = extractBaseClass(typeString)
    if (typeString.contains("java")) {
      JavaClassesUsed += typeString
    }
//    if (typeString.contains("java.lang")) {
//      return Immutable
//    } else {
//      return MutabilityUnknown
//    }

    if (baseClass == NoType) {
      Immutable
    } else if (ScalaAssumedImmutableTypes.contains(baseClass) || ImmutableTypes.contains(baseClass) || ImmutablePackageAliases.contains(baseClass) || PreDefAliases.contains(baseClass) || ExceptionPackageAliases.contains(baseClass) || NumericPackageAliases.contains(baseClass)) {
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
