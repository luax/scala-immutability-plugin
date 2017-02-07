package components

import cell.CellCompleter
import helpers.Utils
import immutability._

import scala.collection.mutable.{ListBuffer, Map, Set}
import scala.tools.nsc._
import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}

class ReporterComponent(val global: Global, val phaseName: String, val runsAfterPhase: String, val scanComponent: ScanComponent, val mutabilityComponent: MutabilityComponent) extends NscPluginComponent {
  import global._

  override val runsAfter = List(runsAfterPhase)

  var hasRun = false

  class Data() {

    class Template () {
      var objects: Set[mutabilityComponent.global.Symbol] = Set()
      var caseObjects: Set[mutabilityComponent.global.Symbol] = Set()
      var traits: Set[mutabilityComponent.global.Symbol] = Set()
      var classes: Set[mutabilityComponent.global.Symbol] = Set()
      var caseClasses: Set[mutabilityComponent.global.Symbol] = Set()
      var anonymousClasses: Set[mutabilityComponent.global.Symbol] = Set()
    }
    class Mutable extends Template
    class ShallowImmutable extends Template
    class DeeplyImmutable extends Template
    class ConditionallyImmutable extends Template

    val mutable = new Mutable()
    val deeplyImmutable = new DeeplyImmutable()
    val shallowImmutable = new ShallowImmutable()
    val conditionallyImmutable = new ConditionallyImmutable()

    def conditionallyImmutableInheritance(classesThatExtendWithTypeArguments: Map[scanComponent.global.Symbol, Set[scanComponent.global.Type]]) = {
      findConditionallyImmutableWithShallowOrMutable(classesThatExtendWithTypeArguments).foreach((x) => {
        val klass = x._1
        val immutability = x._2
        val parent = x._3
        val field = x._3
        val typeArgument = x._4
        val typeArgumentCellCompleter = x._5
        // println(s"Found a class '${klass}' (with immutability property ${immutability}) that extends a klass '${parent.typeSymbol.fullName}' (that is conditionally immutable) using a type argument with immutability property '${typeArgumentCellCompleter.cell.getResult()}' (${typeArgument.typeSymbol.fullName})")
        categorize(klass, immutability)
      })
    }

    def conditionallyImmutableFields(classesThatHaveFieldsWithTypeArguments: Map[scanComponent.global.Symbol, Set[scanComponent.global.Type]]) = {
      findConditionallyImmutableWithShallowOrMutable(classesThatHaveFieldsWithTypeArguments).foreach((x) => {
        val klass = x._1
        val immutability = x._2
        val field = x._3
        val typeArgument = x._4
        val typeArgumentCellCompleter = x._5
        // println(s"Found a class '${klass}' (with immutability property ${immutability}) that had a field '${field}' that used a type with immutability property '${typeArgumentCellCompleter.cell.getResult()}' (${typeArgument.typeSymbol.fullName}) on a conditionally immutable class (${field.typeSymbol})")
        categorize(klass, immutability)
      })
    }

    def findConditionallyImmutableWithShallowOrMutable(map: Map[scanComponent.global.Symbol, Set[scanComponent.global.Type]]) = {
      map.flatMap(x => {
        val klass = x._1.asInstanceOf[mutabilityComponent.global.Symbol]
        val types = x._2
        var values = new ListBuffer[(mutabilityComponent.global.Symbol, Immutability, scanComponent.global.Type, scanComponent.global.Type, CellCompleter[ImmutabilityKey.type, Immutability])]()
        types.foreach(tpe => {
          tpe.typeArgs.foreach(typeArgument => {
            val conditionallyImmutable = mutabilityComponent.classToCellCompleter.get(tpe.typeSymbol.asInstanceOf[mutabilityComponent.global.Symbol]) match {
              case Some(cellCompleter) => cellCompleter.cell.getResult == ConditionallyImmutable
              case _ => false
            }
            if (conditionallyImmutable) {
              mutabilityComponent.classToCellCompleter.get(typeArgument.typeSymbol.asInstanceOf[mutabilityComponent.global.Symbol]) match {
                case Some(typeArgumentCellCompleter) =>
                  if (typeArgumentCellCompleter.cell.getResult() == Mutable || typeArgumentCellCompleter.cell.getResult() == ShallowImmutable) {
                    val immutability = mutabilityComponent.classToCellCompleter.getOrElse(klass.asInstanceOf[mutabilityComponent.global.Symbol], null).cell.getResult()
                    values += Tuple5(klass, immutability, tpe, typeArgument, typeArgumentCellCompleter)
                  }
                case _ =>
              }
            }
          })
        })
        values.toList
      })
    }

    def categorize (klass: mutabilityComponent.global.Symbol, immutability: Immutability): Unit = {
      if (klass.isModuleClass) {
        if (klass hasFlag Flag.CASE) {
          immutability match {
            case Immutable => deeplyImmutable.caseObjects += klass
            case ShallowImmutable => shallowImmutable.caseObjects += klass
            case Mutable => mutable.caseObjects += klass
            case ConditionallyImmutable => conditionallyImmutable.caseObjects += klass
            case _ =>
          }
        } else {
          immutability match {
            case Immutable => deeplyImmutable.objects += klass
            case ShallowImmutable => shallowImmutable.objects += klass
            case Mutable => mutable.objects += klass
            case ConditionallyImmutable => conditionallyImmutable.objects += klass
            case _ =>
          }
        }
      } else if (klass.isAnonymousClass) {
        immutability match {
          case Immutable => deeplyImmutable.anonymousClasses += klass
          case ShallowImmutable => shallowImmutable.anonymousClasses += klass
          case Mutable => mutable.anonymousClasses += klass
          case ConditionallyImmutable => conditionallyImmutable.anonymousClasses += klass
          case _ =>
        }
      } else {
        if (klass hasFlag Flag.TRAIT) {
          immutability match {
            case Immutable => deeplyImmutable.traits += klass
            case ShallowImmutable => shallowImmutable.traits += klass
            case Mutable => mutable.traits += klass
            case ConditionallyImmutable => conditionallyImmutable.traits += klass
            case _ =>
          }
        } else if (klass hasFlag Flag.CASE) {
          immutability match {
            case Immutable => deeplyImmutable.caseClasses += klass
            case ShallowImmutable => shallowImmutable.caseClasses += klass
            case Mutable => mutable.caseClasses += klass
            case ConditionallyImmutable => conditionallyImmutable.caseClasses += klass
            case _ =>
          }
        } else {
          immutability match {
            case Immutable => deeplyImmutable.classes += klass
            case ShallowImmutable => shallowImmutable.classes += klass
            case Mutable => mutable.classes += klass
            case ConditionallyImmutable => conditionallyImmutable.classes += klass
            case _ =>
          }
        }
      }
    }
  }

  def notifyTest(): Unit = {
    for ((klass, v) <- mutabilityComponent.classToCellCompleter) {
      val classSymbol = klass.asInstanceOf[Symbol]
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
        case ConditionallyImmutable => {
          if (notifyTest(classSymbol.pos, classSymbol, Utils.IsConditionallyImmutable)) {
            return
          }
        }
        case _ => None
      }
    }
  }

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

  def printStats(): Unit = {
    println("##########################")
    println("##########################")
    println("## Plugin ran successfully")
    println("##########################")
    println("##########################")
    println("## Templates (including private, abstract, sealed etc)")
    println("# templates found: " + scanComponent.templates.size)
    println("# classes found: " + scanComponent.classes.size)
    println("# case classes found: " + scanComponent.caseClasses.size)
    println("# anonymous classes found: " + scanComponent.anonymousClasses.size)
    println("# traits found: " + scanComponent.traits.size)
    println("# objects found: " + scanComponent.objects.size)
    println("# case objects found: " + scanComponent.objects.size)
    println("## Fields")
    println("#templates with var: " + scanComponent.classesWithVar.size)
    println("#templates with val: " + scanComponent.classesWithVal.size)
    println("#templates with only val: " + (scanComponent.classesWithVal -- scanComponent.classesWithVar).size)

    immutabilityStatistics()
    reasonsCount()
    conditionallyImmutableInheritance()
    conditionallyImmutableField()

    println("## Reasons map (class -> reason):")
    println(mutabilityComponent.ImmutabilityReasons.toList.map(x => s"${x._1} -> ${x._2}").mkString("\n"))
    println("-")
    println("## Unidentified classes:")
    println(Assumptions.UnidentifiedTypes.mkString("\n"))
    println("## Java classes used:")
    println((Assumptions.JavaClassesUsed -- Assumptions.JavaAssumedImmutableTypes).mkString("\n"))
    println("-")

    println("-")
    println("## Classes without cell completer: ")
    printSymbols(mutabilityComponent.classesWithoutCellCompleter)
    println("## Assignments without cell completer: ")
    printSymbols(mutabilityComponent.assignmentWithoutCellCompleter)

    println("##########################")
    println("##########################")
  }

  def immutabilityStatistics() = {
    val data = new Data()
    for ((k, v) <- mutabilityComponent.classToCellCompleter) {
      val immutability = v.cell.getResult
      val klass = k.asInstanceOf[mutabilityComponent.global.Symbol]
      data.categorize(klass, immutability)
    }

    println("-")
    println("## Immutability statistics")
    println("-")
    println("#mutable objects: " + data.mutable.objects.size)
    printSymbols(data.mutable.objects)

    println("#mutable traits: " + data.mutable.traits.size)
    printSymbols(data.mutable.traits)

    println("#mutable case classes: " + data.mutable.caseClasses.size)
    printSymbols(data.mutable.caseClasses)

    println("#mutable case objects: " + data.mutable.caseObjects.size)
    printSymbols(data.mutable.caseObjects)

    println("#mutable classes: " + data.mutable.classes.size)
    printSymbols(data.mutable.classes)

    println("#mutable anonymous: " + data.mutable.anonymousClasses.size)
    printSymbols(data.mutable.anonymousClasses)
    println("-")

    println("#shallow objects: " + data.shallowImmutable.objects.size)
    printSymbols(data.shallowImmutable.objects)

    println("#shallow traits: " + data.shallowImmutable.traits.size)
    printSymbols(data.shallowImmutable.traits)

    println("#shallow case classes: " + data.shallowImmutable.caseClasses.size)
    printSymbols(data.shallowImmutable.caseClasses)

    println("#shallow case objects: " + data.shallowImmutable.caseObjects.size)
    printSymbols(data.shallowImmutable.caseObjects)

    println("#shallow classes: " + data.shallowImmutable.classes.size)
    printSymbols(data.shallowImmutable.classes)

    println("#shallow anonymous: " + data.shallowImmutable.anonymousClasses.size)
    printSymbols(data.shallowImmutable.anonymousClasses)
    println("-")

    println("#immutable objects: " + data.deeplyImmutable.objects.size)
    printSymbols(data.deeplyImmutable.objects)

    println("#immutable traits: " + data.deeplyImmutable.traits.size)
    printSymbols(data.deeplyImmutable.traits)

    println("#immutable case classes: " + data.deeplyImmutable.caseClasses.size)
    printSymbols(data.deeplyImmutable.caseClasses)

    println("#immutable case objects: " + data.deeplyImmutable.caseObjects.size)
    printSymbols(data.deeplyImmutable.caseObjects)

    println("#immutable classes: " + data.deeplyImmutable.classes.size)
    printSymbols(data.deeplyImmutable.classes)

    println("#immutable anonymous: " + data.deeplyImmutable.anonymousClasses.size)
    printSymbols(data.deeplyImmutable.anonymousClasses)
    println("-")

    println("#conditionally immutable objects: " + data.conditionallyImmutable.objects.size)
    printSymbols(data.conditionallyImmutable.objects)

    println("#conditionally immutable traits: " + data.conditionallyImmutable.traits.size)
    printSymbols(data.conditionallyImmutable.traits)

    println("#conditionally immutable case classes: " + data.conditionallyImmutable.caseClasses.size)
    printSymbols(data.conditionallyImmutable.caseClasses)

    println("#conditionally immutable case objects: " + data.conditionallyImmutable.caseObjects.size)
    printSymbols(data.conditionallyImmutable.caseObjects)

    println("#conditionally immutable classes: " + data.conditionallyImmutable.classes.size)
    printSymbols(data.conditionallyImmutable.classes)

    println("#conditionally immutable anonymous: " + data.conditionallyImmutable.anonymousClasses.size)
    printSymbols(data.conditionallyImmutable.anonymousClasses)
    println("-")

    println("-")
    println("## Immutability statistics matrix")
    println(s"(Template) \t Mutable \t Shallow immutable \t Deeply immutable \t Conditionally deeply immutable")
    printDataRows(data)
    println("-")
  }

  def printSymbols(set: Set[mutabilityComponent.global.Symbol]): Unit = {
    println(set.toList.map((s) => {
      val c = mutabilityComponent.classToCellCompleter.getOrElse(s, null)
      if (c == null) {
        s.fullName + " => " + "no_cell_completer"
      } else {
        s.fullName + " => " + c.cell.getResult() + " (immutability reason: '" + mutabilityComponent.ImmutabilityReasons.getOrElse(s, "no contradiction") + "')"
      }
    }).sorted.mkString("\n"))
  }

  def printDataRows(data: Data): Unit = {
    println(s"Class \t ${data.mutable.classes.size} \t ${data.shallowImmutable.classes.size} \t ${data.deeplyImmutable.classes.size} \t ${data.conditionallyImmutable.classes.size}")
    println(s"Case class \t ${data.mutable.caseClasses.size} \t ${data.shallowImmutable.caseClasses.size} \t ${data.deeplyImmutable.caseClasses.size} \t ${data.conditionallyImmutable.caseClasses.size}")
    println(s"Anonymous class \t ${data.mutable.anonymousClasses.size} \t ${data.shallowImmutable.anonymousClasses.size} \t ${data.deeplyImmutable.anonymousClasses.size} \t ${data.conditionallyImmutable.anonymousClasses.size}")
    println(s"Trait \t ${data.mutable.traits.size} \t ${data.shallowImmutable.traits.size} \t ${data.deeplyImmutable.traits.size} \t ${data.conditionallyImmutable.traits.size}")
    println(s"Object \t ${data.mutable.objects.size} \t ${data.shallowImmutable.objects.size} \t ${data.deeplyImmutable.objects.size} \t ${data.conditionallyImmutable.objects.size}")
    println(s"Case object \t ${data.mutable.caseObjects.size} \t ${data.shallowImmutable.caseObjects.size} \t ${data.deeplyImmutable.caseObjects.size} \t ${data.conditionallyImmutable.caseObjects.size}")
  }

  def conditionallyImmutableInheritance() = {
    val data = new Data()
    data.conditionallyImmutableInheritance(scanComponent.classesThatExtendWithTypeArguments)
    println("## Classes that extend conditionally immutable with shallow/mutable argument")
    println(s"Type that extend ⬇️️ | Type's immutability ➡️️ \t Mutable \t Shallow immutable \t Deeply immutable \t Conditionally deeply immutable")
    printDataRows(data)
    println("-")
  }

  def conditionallyImmutableField() = {
    val data = new Data()
    data.conditionallyImmutableFields(scanComponent.classesThatHaveFieldsWithTypeArguments)
    println("## Classes that has a field that use a conditionally immutable with shallow/mutable argument")
    println(s"Type with field ⬇️️ | Type's immutability ➡️️ \t Mutable \t Shallow immutable \t Deeply immutable \t Conditionally deeply immutable")
    printDataRows(data)
    println("-")
  }

  def reasonsCount() = {
    println("-")
    println("## Reasons attribute count (mutable)")
    println(
      mutabilityComponent.ImmutabilityReasonsMutable.map(x => {
        val reasons = x._2
        val bar = reasons.toList.map(mutabilityComponent.REASONS_MAPPING.getOrElse(_, null)).sorted.mkString(" ")
        s"${getSymbolType(x._1.asInstanceOf[Symbol])}\t${bar}"
      }).groupBy(identity)
        .mapValues(_.size)
        .toList
        .map(x => s"${x._1}\t${x._2}")
        .sorted
        .mkString("\n")
    )
    println("-")
    println("## Reasons attribute count (shallow)")
    println(
      mutabilityComponent.ImmutabilityReasonsShallow.map(x => {
        val reasons = x._2
        val bar = reasons.toList.map(mutabilityComponent.REASONS_MAPPING.getOrElse(_, null)).sorted.mkString(" ")
        s"${getSymbolType(x._1.asInstanceOf[Symbol])}\t${bar}"
      }).groupBy(identity)
        .mapValues(_.size)
        .toList
        .map(x => s"${x._1}\t${x._2}")
        .sorted
        .mkString("\n")
    )
  }

  def getSymbolType(sym: Symbol): String = {
    if (sym.isModuleClass) {
      if (sym hasFlag Flag.CASE) {
        "Case object"
      } else {
        "Objects"
      }
    } else if (sym.isAnonymousClass) {
      "Anonymous classes"
    } else {
      if (sym hasFlag Flag.TRAIT) {
        "Traits"
      } else if (sym hasFlag Flag.CASE) {
        "Case classes"
      } else {
        "Classes"
      }
    }
  }

  override def newPhase(prev: Phase): StdPhase = new NewPhase(prev)

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
        printStats()
      }
    }
  }
}
