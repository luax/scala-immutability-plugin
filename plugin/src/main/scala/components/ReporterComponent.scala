package components

import helpers.Utils
import immutability.{Immutable, KnownObjects, Mutable, ShallowImmutable}

import scala.collection.immutable.List
import scala.tools.nsc._
import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}

class ReporterComponent(val global: Global, val phaseName: String, val runsAfterPhase: String, val scanComponent: ScanComponent, val mutabilityComponent: MutabilityComponent) extends NscPluginComponent {

  import global._

  override val runsAfter = List(runsAfterPhase)

  var hasRun = false

  def notifyTest(pos: Position, klass: Symbol, message: String): Boolean = {
    val testMessage = Utils.getCurrentTestMessage
    val errorMessage = Utils.mutabilityMessage(klass.name.toString, message)
    if (testMessage == errorMessage) {
      Utils.log(s"Reporting '$errorMessage'")
      // False negative error to notify test that it was successful
      global.reporter.error(pos, errorMessage)
      true
    } else {
      Utils.log(s"Tried to report '$errorMessage' but want '$testMessage'")
      false
    }
  }

  def notifyTest(): Unit = {
    for ((klass, v) <- mutabilityComponent.classToCellCompleter) {
      val classSymbol = klass.asInstanceOf[Symbol]
      println(classSymbol, v.cell.getResult)
      v.cell.getResult match {
        case Mutable => {
          if (notifyTest(classSymbol.pos, classSymbol, Utils.IsMutable)) {
            return // Break loop
          }
        }
        case ShallowImmutable => {
          if (notifyTest(classSymbol.pos, classSymbol, Utils.IsShallowImmutable)) {
            return
          }
        }
        case Immutable => {
          if (notifyTest(classSymbol.pos, classSymbol, Utils.IsDeeplyImmutable)) {
            return
          }
        }
        case _ => None
      }
    }
  }

  class NewPhase(prev: Phase) extends StdPhase(prev) {
    override def apply(unit: CompilationUnit): Unit = {
      if (Utils.isScalaTest) {
        // For testing
        Utils.getPool.whileQuiescentResolveDefault
        Utils.getPool.shutdown()
        notifyTest()
        Utils.newPool
      } else if (!hasRun) {
        hasRun = true
        Utils.getPool.whileQuiescentResolveDefault
        Utils.getPool.shutdown()
        stats()
      }
    }
  }

  def stats(): Unit = {
    print("Plugin ran successfully")
    print("-")
    print("#classes found: " + scanComponent.classes.size)
    print("#case classes found: " + scanComponent.caseClasses.size)
    print("#abstract classes found: " + scanComponent.abstractClasses.size)
    print("#traits found: " + scanComponent.traits.size)
    print("#objects found: " + scanComponent.objects.size)
    print("#templs found: " + scanComponent.templates.size) // TODO
    print("-")


    print("Fields")
    print("#classes with var: " + scanComponent.classesWithVar.size)
    print(scanComponent.classesWithVar.foldLeft("") { (r, s) => r + "[" + s + "]" + " " })
    print("#classes with val: " + scanComponent.classesWithVal.size)
    print(scanComponent.classesWithVal.foldLeft("") { (r, s) => r + "[" + s + "]" + " " })

    val classesWithOnlyVal = scanComponent.classesWithVal -- scanComponent.classesWithVar
    print("#classes with only val: " + classesWithOnlyVal.size)
    print(classesWithOnlyVal.foldLeft("") { (r, s) => r + "[" + s + "]" + " " })
    print("-")



    // module (object)
    // trait
    // case
    // abstract
    // class
    var mutableObjects: List[Symbol] = List()
    var mutableTraits: List[Symbol] = List()
    var mutableCaseClasses: List[Symbol] = List()
    var mutableAbstractClasses: List[Symbol] = List()
    var mutableClasses: List[Symbol] = List()

    var shallowImmutableObjects: List[Symbol] = List()
    var shallowImmutableTraits: List[Symbol] = List()
    var shallowImmutableCaseClasses: List[Symbol] = List()
    var shallowImmutableAbstractClasses: List[Symbol] = List()
    var shallowImmutableClasses: List[Symbol] = List()

    var immutableObjects: List[Symbol] = List()
    var immutableTraits: List[Symbol] = List()
    var immutableCaseClasses: List[Symbol] = List()
    var immutableAbstractClasses: List[Symbol] = List()
    var immutableClasses: List[Symbol] = List()

    for ((klass, v) <- mutabilityComponent.classToCellCompleter) {
      val immutability = v.cell.getResult
      if (immutability == Mutable) {

        val k = klass.asInstanceOf[Symbol]
        if (k.isModuleClass) {
          mutableObjects ::= k
        } else {
          if (k hasFlag Flag.TRAIT) {
            mutableTraits ::= k
          } else if (klass hasFlag Flag.CASE) {
            mutableCaseClasses ::= k
          } else if (klass hasFlag Flag.ABSTRACT) {
            mutableAbstractClasses ::= k
          } else {
            mutableClasses ::= k
          }
        }
      } else if (immutability == Immutable) {
        val k = klass.asInstanceOf[Symbol]
        if (k.isModuleClass) {
          immutableObjects ::= k
        } else {
          if (k hasFlag Flag.TRAIT) {
            immutableTraits ::= k
          } else if (klass hasFlag Flag.CASE) {
            immutableCaseClasses ::= k
          } else if (klass hasFlag Flag.ABSTRACT) {
            immutableAbstractClasses ::= k
          } else {
            immutableClasses ::= k
          }
        }
      } else if (immutability == ShallowImmutable) {
        val k = klass.asInstanceOf[Symbol]
        if (k.isModuleClass) {
          shallowImmutableObjects ::= k
        } else {
          if (k hasFlag Flag.TRAIT) {
            shallowImmutableTraits ::= k
          } else if (klass hasFlag Flag.CASE) {
            shallowImmutableCaseClasses ::= k
          } else if (klass hasFlag Flag.ABSTRACT) {
            shallowImmutableAbstractClasses ::= k
          } else {
            shallowImmutableClasses ::= k
          }
        }
      }
    }

    // module (object)
    // trait
    // case
    // abstract
    // class
    print("#mutable objects: " + mutableObjects.size)
    printCollection(mutableObjects)

    print("#mutable traits: " + mutableTraits.size)
    printCollection(mutableTraits)

    print("#mutable case: " + mutableCaseClasses.size)
    printCollection(mutableCaseClasses)

    print("#mutable abstract: " + mutableAbstractClasses.size)
    printCollection(mutableTraits)

    print("#mutable classes: " + mutableClasses.size)
    printCollection(mutableClasses)
    print("-")

    //////////////
    print("#shallow objects: " + shallowImmutableObjects.size)
    printCollection(shallowImmutableObjects)

    print("#shallow traits: " + shallowImmutableTraits.size)
    printCollection(shallowImmutableTraits)

    print("#shallow case: " + shallowImmutableCaseClasses.size)
    printCollection(shallowImmutableCaseClasses)

    print("#shallow abstract: " + shallowImmutableAbstractClasses.size)
    printCollection(shallowImmutableTraits)

    print("#shallow classes: " + shallowImmutableClasses.size)
    printCollection(shallowImmutableClasses)
    print("-")

    ////////
    print("#immutable objects: " + immutableObjects.size)
    printCollection(immutableObjects)

    print("#immutable traits: " + immutableTraits.size)
    printCollection(immutableTraits)

    print("#immutable case: " + immutableCaseClasses.size)
    printCollection(immutableCaseClasses)

    print("#immutable abstract: " + immutableAbstractClasses.size)
    printCollection(immutableTraits)

    print("#immutable classes: " + immutableClasses.size)
    printCollection(immutableClasses)
    print("-")

    print("-")
    print("#classes without cell completer: ")
    printCollection(mutabilityComponent.classesWithoutCellCompleter.toList.asInstanceOf[List[Symbol]])
    print("#assignments without cell completer: ")
    printCollection(mutabilityComponent.assignmentWithoutCellCompleter.toList.asInstanceOf[List[Symbol]])
    print("-")

    print("What is the percentage of classes/traits that are shallow/deep immutable?")
    val percentageMutableClasses = mutableClasses.size.toDouble / scanComponent.classes.size * 100
    val percentageShallowImmutableClasses = shallowImmutableClasses.size.toDouble / scanComponent.classes.size * 100
    val percentageImmutableClasses = immutableClasses.size.toDouble / scanComponent.classes.size * 100
    print(s"Mutable classes: $percentageMutableClasses %")
    print(s"Shallow classes: $percentageShallowImmutableClasses %")
    print(s"Deep classes: $percentageImmutableClasses %")

    val percentageMutableTraits = mutableTraits.size.toDouble / scanComponent.traits.size * 100
    val percentageShallowImmutableTraits = shallowImmutableTraits.size.toDouble / scanComponent.traits.size * 100
    val percentageImmutableTraits = immutableTraits.size.toDouble / scanComponent.traits.size * 100
    print(s"Mutable traits: $percentageMutableTraits %")
    print(s"Shallow traits: $percentageShallowImmutableTraits %")
    print(s"Deep traits: $percentageImmutableTraits %")

    print("-")
    print("What is the percentage of case classes that are shallow/deep immutable?")
    print("-")
    print("What is the percentage of singleton objects that are shallow/deep immutable?")

    print("-")
    print("Reasons:")
    println(mutabilityComponent.reasons)
    print("Java")
    println(KnownObjects.JavaClassesUsed)
  }

  def print(msg: String): Unit = {
    if (Utils.isScalaTest) {
      Utils.log(msg)
    } else {
      reporter.echo(msg)
    }
  }

  def printCollection(list: List[Symbol]): Unit = {
    print(list.foldLeft("") { (r, s) => r + "[(" + s + "," + s.tpe.underlying + ")]" + " " })
  }

  override def newPhase(prev: Phase): StdPhase = new NewPhase(prev)
}
