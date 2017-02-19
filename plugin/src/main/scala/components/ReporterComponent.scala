package components

import cell.CellCompleter
import helpers.Utils
import immutability._

import scala.collection.mutable.{ListBuffer, Map, Set}
import scala.tools.nsc._
import scala.tools.nsc.plugins.{PluginComponent => NscPluginComponent}

class ReporterComponent(val global: Global, val phaseName: String, val runsAfterPhase: String, val scanComponent: ScanComponent, val mutabilityComponent: MutabilityComponent) extends NscPluginComponent {

  import global._

  override val runsAfter = List(runsAfterPhase)

  var hasRun = false

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
    val data = new Data()
    for ((k, v) <- mutabilityComponent.classToCellCompleter) {
      val immutability = v.cell.getResult
      val klass = k.asInstanceOf[mutabilityComponent.global.Symbol]
      data.categorize(klass, immutability)
    }
    immutabilityMatrix(data)
    println
    reasonsCount(data)
    println
    conditionallyImmutableInheritance
    println
    conditionallyImmutableField
    println
    immutabilityStatistics(data)
    // println
    // reasonsMap
    println
    classesInfo
    println
    cellCompleterInfo
    // TODO:
    // Below is wrong...
    // println("## Templates (including private, abstract, sealed etc)")
    // println("Classes found\t" + scanComponent.classes.size)
    // println("Case classes found\t" + scanComponent.caseClasses.size)
    // println("Anonymous classes found\t" + scanComponent.anonymousClasses.size)
    // println("Traits found\t" + scanComponent.traits.size)
    // println("Objects found\t" + scanComponent.objects.size)
    // println("Case objects found\t" + scanComponent.caseObjects.size)
    // println("## Fields")
    // println("Templates with var\t" + scanComponent.classesWithVar.size)
    // println("Templates with val\t" + scanComponent.classesWithVal.size)
    // println("Templates with only val\t" + (scanComponent.classesWithVal -- scanComponent.classesWithVar).size)
  }

  def classesInfo = {
    println("## Unidentified classes:")
    println(Assumptions.UnidentifiedTypes.mkString("\n"))
    println("## Java classes used:")
    println((Assumptions.JavaClassesUsed -- Assumptions.JavaAssumedImmutableTypes).mkString("\n"))
  }

  def cellCompleterInfo = {
    println("## Classes without cell completer: ")
    printSymbols(mutabilityComponent.classesWithoutCellCompleter)
    println("## Assignments without cell completer: ")
    printSymbols(mutabilityComponent.assignmentWithoutCellCompleter)
  }

  def immutabilityStatistics(data: Data) = {
    println("### Immutability statistics")
    println("## Mutable")
    println("# objects: " + data.mutable.objects.size)
    printSymbols(data.mutable.objects)

    println("# traits: " + data.mutable.traits.size)
    printSymbols(data.mutable.traits)

    println("# case classes: " + data.mutable.caseClasses.size)
    printSymbols(data.mutable.caseClasses)

    println("# case objects: " + data.mutable.caseObjects.size)
    printSymbols(data.mutable.caseObjects)

    println("# classes: " + data.mutable.classes.size)
    printSymbols(data.mutable.classes)

    println("# anonymous: " + data.mutable.anonymousClasses.size)
    printSymbols(data.mutable.anonymousClasses)

    println("## Shallow")
    println("# objects: " + data.shallowImmutable.objects.size)
    printSymbols(data.shallowImmutable.objects)

    println("# traits: " + data.shallowImmutable.traits.size)
    printSymbols(data.shallowImmutable.traits)

    println("# case classes: " + data.shallowImmutable.caseClasses.size)
    printSymbols(data.shallowImmutable.caseClasses)

    println("# case objects: " + data.shallowImmutable.caseObjects.size)
    printSymbols(data.shallowImmutable.caseObjects)

    println("# classes: " + data.shallowImmutable.classes.size)
    printSymbols(data.shallowImmutable.classes)

    println("# anonymous: " + data.shallowImmutable.anonymousClasses.size)
    printSymbols(data.shallowImmutable.anonymousClasses)
    println

    println("## Immutable")
    println("# objects: " + data.deeplyImmutable.objects.size)
    printSymbols(data.deeplyImmutable.objects)

    println("# traits: " + data.deeplyImmutable.traits.size)
    printSymbols(data.deeplyImmutable.traits)

    println("# case classes: " + data.deeplyImmutable.caseClasses.size)
    printSymbols(data.deeplyImmutable.caseClasses)

    println("# case objects: " + data.deeplyImmutable.caseObjects.size)
    printSymbols(data.deeplyImmutable.caseObjects)

    println("# classes: " + data.deeplyImmutable.classes.size)
    printSymbols(data.deeplyImmutable.classes)

    println("# anonymous: " + data.deeplyImmutable.anonymousClasses.size)
    printSymbols(data.deeplyImmutable.anonymousClasses)
    println

    println("## Conditionally immutable")
    println("# objects: " + data.conditionallyImmutable.objects.size)
    printSymbols(data.conditionallyImmutable.objects)

    println("# traits: " + data.conditionallyImmutable.traits.size)
    printSymbols(data.conditionallyImmutable.traits)

    println("# case classes: " + data.conditionallyImmutable.caseClasses.size)
    printSymbols(data.conditionallyImmutable.caseClasses)

    println("# case objects: " + data.conditionallyImmutable.caseObjects.size)
    printSymbols(data.conditionallyImmutable.caseObjects)

    println("# classes: " + data.conditionallyImmutable.classes.size)
    printSymbols(data.conditionallyImmutable.classes)

    println("# anonymous: " + data.conditionallyImmutable.anonymousClasses.size)
    printSymbols(data.conditionallyImmutable.anonymousClasses)
  }

  def printSymbols(set: Set[mutabilityComponent.global.Symbol]): Unit = {
    println(set.toList.map((s) => {
      val c = mutabilityComponent.classToCellCompleter.getOrElse(s, null)
      if (c == null) {
        s.fullName + " => " + "no_cell_completer"
      } else {
        val immutability = c.cell.getResult
        s.fullName + " => " + immutability + " (immutability reason: '" + mutabilityComponent.ImmutabilityReason.getOrElse(s, "no contradiction") + " " + mutabilityComponent.getReasons(s, immutability).mkString(" ") + "')"
      }
    }).sorted.mkString("\n"))
  }

  def immutabilityMatrix(data: Data): Unit = {
    println("## Immutability statistics matrix")
    println(s"(Template) \t Mutable \t Shallow immutable \t Deeply immutable \t Conditionally deeply immutable")
    printDataRows(data)
  }

  def conditionallyImmutableInheritance() = {
    val data = new Data()
    data.conditionallyImmutableInheritance(scanComponent.classesThatExtendWithTypeArguments)
    println("## Classes that extend conditionally immutable with shallow/mutable argument")
    println(s"Type that extend ⬇️️ | Type's immutability ➡️️ \t Mutable \t Shallow immutable \t Deeply immutable \t Conditionally deeply immutable")
    printDataRows(data)
  }

  def conditionallyImmutableField() = {
    val data = new Data()
    data.conditionallyImmutableFields(scanComponent.classesThatHaveFieldsWithTypeArguments)
    println("## Classes that has a field that use a conditionally immutable with shallow/mutable argument")
    println(s"Type with field ⬇️️ | Type's immutability ➡️️ \t Mutable \t Shallow immutable \t Deeply immutable \t Conditionally deeply immutable")
    printDataRows(data)
  }

  def printDataRows(data: Data): Unit = {
    println(s"Class \t ${data.mutable.classes.size} \t ${data.shallowImmutable.classes.size} \t ${data.deeplyImmutable.classes.size} \t ${data.conditionallyImmutable.classes.size}")
    println(s"Case class \t ${data.mutable.caseClasses.size} \t ${data.shallowImmutable.caseClasses.size} \t ${data.deeplyImmutable.caseClasses.size} \t ${data.conditionallyImmutable.caseClasses.size}")
    println(s"Anonymous class \t ${data.mutable.anonymousClasses.size} \t ${data.shallowImmutable.anonymousClasses.size} \t ${data.deeplyImmutable.anonymousClasses.size} \t ${data.conditionallyImmutable.anonymousClasses.size}")
    println(s"Trait \t ${data.mutable.traits.size} \t ${data.shallowImmutable.traits.size} \t ${data.deeplyImmutable.traits.size} \t ${data.conditionallyImmutable.traits.size}")
    println(s"Object \t ${data.mutable.objects.size} \t ${data.shallowImmutable.objects.size} \t ${data.deeplyImmutable.objects.size} \t ${data.conditionallyImmutable.objects.size}")
    println(s"Case object \t ${data.mutable.caseObjects.size} \t ${data.shallowImmutable.caseObjects.size} \t ${data.deeplyImmutable.caseObjects.size} \t ${data.conditionallyImmutable.caseObjects.size}")
  }

  def reasonsCount(data: Data) = {
    val includeName = false
    println("## Reasons table")
    println("Attribute \t Description")
    (mutabilityComponent.MUTABLE_REASONS ++ mutabilityComponent.SHALLOW_REASONS).foreach((s) => println(s + "\t" + mutabilityComponent.REASONS_MAPPING_INV(s)))
    println
    println("## Reasons by template (mutable)")
    val mutableReasonsByTemplate = reasons(Mutable)
    println(mutableReasonsByTemplate._1)
    assert(mutableReasonsByTemplate._2 == data.mutableSize)
    println
    println("## Reasons by template (shallow)")
    val shallowReasonsByTemplate = reasons(ShallowImmutable)
    println(shallowReasonsByTemplate._1)
    assert(shallowReasonsByTemplate._2 == data.shallowImmutableSize)
    println
    println("## Reasons (mutable)")
    val mutableReasons = reasons(Mutable, includeName)
    println(mutableReasons._1)
    assert(mutableReasons._2 == data.mutableSize)
    println
    println("## Reasons (shallow)")
    val shallowReasons = reasons(ShallowImmutable, includeName)
    println(shallowReasons._1)
    assert(shallowReasons._2 == data.shallowImmutableSize)
    println
    println("## Reasons (mutable and shallow)")
    val mutableAndShallowReasons = reasons(null, includeName)
    println(mutableAndShallowReasons._1)
    assert(mutableAndShallowReasons._2 == (data.mutableSize + data.shallowImmutableSize))
  }

  def reasons(immutability: Immutability, includeTemplateName: Boolean = true) = {
    var count = 0
    val reasons = mutabilityComponent.ImmutabilityReasons.map(x => {
      // Reasons: B C D
      val reasons = mutabilityComponent.getReasons(x._1, immutability)
      if (reasons.nonEmpty) {
        if (includeTemplateName) {
          // Template name: Class, Object, Trait etc.
          val name = getTemplateName(x._1.asInstanceOf[Symbol])
          s"${name}\t${reasons.mkString(" ")}"
        } else {
          s"${reasons.mkString(" ")}"
        }
      } else {
        ""
      }
    }).filter(_.length > 0)
      .groupBy(identity)
      .mapValues(_.size)
      .toList
      .map(x => {
        count += x._2
        s"${x._1}\t${x._2}"
      })
      .sorted
      .mkString("\n")
    (reasons, count)
  }

  def getTemplateName(sym: Symbol): String = {
    if (sym.isModuleClass) {
      if (sym hasFlag Flag.CASE) {
        "Case object"
      } else {
        "Object"
      }
    } else if (sym.isAnonymousClass) {
      "Anonymous class"
    } else {
      if (sym hasFlag Flag.TRAIT) {
        "Trait"
      } else if (sym hasFlag Flag.CASE) {
        "Case class"
      } else {
        "Class"
      }
    }
  }

  def reasonsMap = {
    println("## Reasons map (class -> reason):")
    println(mutabilityComponent.ImmutabilityReason.toList.map(x => s"${x._1} -> ${x._2}").mkString("\n"))
  }

  override def newPhase(prev: Phase): StdPhase = new NewPhase(prev)

  class Data {

    val mutable = new Mutable()
    val deeplyImmutable = new DeeplyImmutable()
    val shallowImmutable = new ShallowImmutable()
    val conditionallyImmutable = new ConditionallyImmutable()

    def mutableSize = {
      mutable.anonymousClasses.size + mutable.caseClasses.size + mutable.caseObjects.size + mutable.classes.size + mutable.objects.size + mutable.traits.size
    }

    def deeplyImmmutableSize = {
      deeplyImmutable.anonymousClasses.size + deeplyImmutable.caseClasses.size + deeplyImmutable.caseObjects.size + deeplyImmutable.classes.size + deeplyImmutable.objects.size + deeplyImmutable.traits.size
    }

    def shallowImmutableSize = {
      shallowImmutable.anonymousClasses.size + shallowImmutable.caseClasses.size + shallowImmutable.caseObjects.size + shallowImmutable.classes.size + shallowImmutable.objects.size + shallowImmutable.traits.size
    }

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

    def categorize(klass: mutabilityComponent.global.Symbol, immutability: Immutability): Unit = {
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

    class Template {
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
        printStats()
      }
    }
  }

}
