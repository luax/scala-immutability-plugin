package components

import helpers.Utils
import immutability._

import scala.collection.mutable.Set
import scala.tools.nsc._
import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}

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
    println("## Classes")
    println("#classes found: " + scanComponent.classes.size)
    println("#case classes found: " + scanComponent.caseClasses.size)
    println("#abstract classes found: " + scanComponent.abstractClasses.size)
    println("#traits found: " + scanComponent.traits.size)
    println("#objects found: " + scanComponent.objects.size)
    println("#templs found: " + scanComponent.templates.size)
    println("-")
    println("Fields")
    println("#classes with var: " + scanComponent.classesWithVar.size)
    println("#classes with val: " + scanComponent.classesWithVal.size)
    println("#classes with only val: " + (scanComponent.classesWithVal -- scanComponent.classesWithVar).size)

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
    var mutableObjects: Set[mutabilityComponent.global.Symbol] = Set()
    var mutableTraits: Set[mutabilityComponent.global.Symbol] = Set()
    var mutableCaseClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var mutableAbstractClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var mutableClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var mutableAnonymousClasses: Set[mutabilityComponent.global.Symbol] = Set()

    var shallowImmutableObjects: Set[mutabilityComponent.global.Symbol] = Set()
    var shallowImmutableTraits: Set[mutabilityComponent.global.Symbol] = Set()
    var shallowImmutableCaseClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var shallowImmutableAbstractClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var shallowImmutableClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var shallowImmutableAnonymousClasses: Set[mutabilityComponent.global.Symbol] = Set()

    var immutableObjects: Set[mutabilityComponent.global.Symbol] = Set()
    var immutableTraits: Set[mutabilityComponent.global.Symbol] = Set()
    var immutableCaseClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var immutableAbstractClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var immutableClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var immutableAnonymousClasses: Set[mutabilityComponent.global.Symbol] = Set()

    var conditionallyImmutableObjects: Set[mutabilityComponent.global.Symbol] = Set()
    var conditionallyImmutableTraits: Set[mutabilityComponent.global.Symbol] = Set()
    var conditionallyImmutableCaseClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var conditionallyImmutableAbstractClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var conditionallyImmutableClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var conditionallyImmutableAnonymousClasses: Set[mutabilityComponent.global.Symbol] = Set()

    for ((k, v) <- mutabilityComponent.classToCellCompleter) {
      val immutability = v.cell.getResult
      val klass = k.asInstanceOf[mutabilityComponent.global.Symbol]
      if (klass.isModuleClass) {
        immutability match {
          case Immutable => immutableObjects += klass
          case ShallowImmutable => shallowImmutableObjects += klass
          case Mutable => mutableObjects += klass
          case ConditionallyImmutable => conditionallyImmutableObjects += klass
          case _ =>
        }
      } else if (klass.isAnonymousClass) {
        immutability match {
          case Immutable => immutableAnonymousClasses += klass
          case ShallowImmutable => shallowImmutableAnonymousClasses += klass
          case Mutable => mutableAnonymousClasses += klass
          case ConditionallyImmutable => conditionallyImmutableAnonymousClasses += klass
          case _ =>
        }
      } else {
        if (klass hasFlag Flag.TRAIT) {
          immutability match {
            case Immutable => immutableTraits += klass
            case ShallowImmutable => shallowImmutableTraits += klass
            case Mutable => mutableTraits += klass
            case ConditionallyImmutable => conditionallyImmutableTraits += klass
            case _ =>
          }
        } else if (klass hasFlag Flag.CASE) {
          immutability match {
            case Immutable => immutableCaseClasses += klass
            case ShallowImmutable => shallowImmutableCaseClasses += klass
            case Mutable => mutableCaseClasses += klass
            case ConditionallyImmutable => conditionallyImmutableCaseClasses += klass
            case _ =>
          }
        } else if (klass hasFlag Flag.ABSTRACT) {
          immutability match {
            case Immutable => immutableAbstractClasses += klass
            case ShallowImmutable => shallowImmutableAbstractClasses += klass
            case Mutable => mutableAbstractClasses += klass
            case ConditionallyImmutable => conditionallyImmutableAbstractClasses += klass
            case _ =>
          }
        } else {
          immutability match {
            case Immutable => immutableClasses += klass
            case ShallowImmutable => shallowImmutableClasses += klass
            case Mutable => mutableClasses += klass
            case ConditionallyImmutable => conditionallyImmutableClasses += klass
            case _ =>
          }
        }
      }
    }

    println("-")
    println("## Immutability statistics")
    println("-")
    println("#mutable objects: " + mutableObjects.size)
    printSymbols(mutableObjects)

    println("#mutable traits: " + mutableTraits.size)
    printSymbols(mutableTraits)

    println("#mutable case: " + mutableCaseClasses.size)
    printSymbols(mutableCaseClasses)

    println("#mutable abstract: " + mutableAbstractClasses.size)
    printSymbols(mutableAbstractClasses)

    println("#mutable classes: " + mutableClasses.size)
    printSymbols(mutableClasses)

    println("#mutable anonymous: " + mutableAnonymousClasses.size)
    printSymbols(mutableAnonymousClasses)
    println("-")

    println("#shallow objects: " + shallowImmutableObjects.size)
    printSymbols(shallowImmutableObjects)

    println("#shallow traits: " + shallowImmutableTraits.size)
    printSymbols(shallowImmutableTraits)

    println("#shallow case: " + shallowImmutableCaseClasses.size)
    printSymbols(shallowImmutableCaseClasses)

    println("#shallow abstract: " + shallowImmutableAbstractClasses.size)
    printSymbols(shallowImmutableAbstractClasses)

    println("#shallow classes: " + shallowImmutableClasses.size)
    printSymbols(shallowImmutableClasses)

    println("#shallow anonymous: " + shallowImmutableAnonymousClasses.size)
    printSymbols(shallowImmutableAnonymousClasses)
    println("-")

    println("#immutable objects: " + immutableObjects.size)
    printSymbols(immutableObjects)

    println("#immutable traits: " + immutableTraits.size)
    printSymbols(immutableTraits)

    println("#immutable case: " + immutableCaseClasses.size)
    printSymbols(immutableCaseClasses)

    println("#immutable abstract: " + immutableAbstractClasses.size)
    printSymbols(immutableAbstractClasses)

    println("#immutable classes: " + immutableClasses.size)
    printSymbols(immutableClasses)

    println("#immutable anonymous: " + immutableAnonymousClasses.size)
    printSymbols(immutableAnonymousClasses)
    println("-")

    println("#conditionally immutable objects: " + immutableObjects.size)
    printSymbols(immutableObjects)

    println("#conditionally immutable traits: " + immutableTraits.size)
    printSymbols(immutableTraits)

    println("#conditionally immutable case: " + immutableCaseClasses.size)
    printSymbols(immutableCaseClasses)

    println("#conditionally immutable  abstract: " + immutableAbstractClasses.size)
    printSymbols(immutableAbstractClasses)

    println("#conditionally immutable classes: " + immutableClasses.size)
    printSymbols(immutableClasses)

    println("#conditionally immutable anonymous: " + conditionallyImmutableAnonymousClasses.size)
    printSymbols(conditionallyImmutableAnonymousClasses)
    println("-")

    println("-")
    println("## Immutability statistics matrix")
    println(s"(Template) \t Mutable \t Shallow immutable \t Deeply immutable \t Conditionally deeply immutable")
    println(s"Classes \t ${mutableClasses.size} \t ${shallowImmutableClasses.size} \t ${immutableClasses.size} \t ${conditionallyImmutableClasses.size}")
    println(s"Case Classes \t ${mutableCaseClasses.size} \t ${shallowImmutableCaseClasses.size} \t ${immutableCaseClasses.size} \t ${conditionallyImmutableCaseClasses.size}")
    println(s"Abstract Classes \t ${mutableAbstractClasses.size} \t ${shallowImmutableAbstractClasses.size} \t ${immutableAbstractClasses.size} \t ${conditionallyImmutableAbstractClasses.size}")
    println(s"Traits \t ${mutableTraits.size} \t ${shallowImmutableTraits.size} \t ${immutableTraits.size} \t ${conditionallyImmutableTraits.size}")
    println(s"Objects \t ${mutableObjects.size} \t ${shallowImmutableObjects.size} \t ${immutableObjects.size} \t ${conditionallyImmutableObjects.size}")
    println(s"Anonymous Classes \t ${mutableAnonymousClasses.size} \t ${shallowImmutableAnonymousClasses.size} \t ${immutableAnonymousClasses.size} \t ${conditionallyImmutableAnonymousClasses.size}")

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

  def conditionallyImmutableInheritance() = {
    var mutableObjects: Set[mutabilityComponent.global.Symbol] = Set()
    var mutableTraits: Set[mutabilityComponent.global.Symbol] = Set()
    var mutableCaseClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var mutableAbstractClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var mutableClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var mutableAnonymousClasses: Set[mutabilityComponent.global.Symbol] = Set()

    var shallowImmutableObjects: Set[mutabilityComponent.global.Symbol] = Set()
    var shallowImmutableTraits: Set[mutabilityComponent.global.Symbol] = Set()
    var shallowImmutableCaseClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var shallowImmutableAbstractClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var shallowImmutableClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var shallowImmutableAnonymousClasses: Set[mutabilityComponent.global.Symbol] = Set()

    var immutableObjects: Set[mutabilityComponent.global.Symbol] = Set()
    var immutableTraits: Set[mutabilityComponent.global.Symbol] = Set()
    var immutableCaseClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var immutableAbstractClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var immutableClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var immutableAnonymousClasses: Set[mutabilityComponent.global.Symbol] = Set()

    var conditionallyImmutableObjects: Set[mutabilityComponent.global.Symbol] = Set()
    var conditionallyImmutableTraits: Set[mutabilityComponent.global.Symbol] = Set()
    var conditionallyImmutableCaseClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var conditionallyImmutableAbstractClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var conditionallyImmutableClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var conditionallyImmutableAnonymousClasses: Set[mutabilityComponent.global.Symbol] = Set()

    scanComponent.classesThatExtendWithTypeArguments.foreach(x => {
      val klass = x._1.asInstanceOf[mutabilityComponent.global.Symbol]
      val parents = x._2
      parents.foreach(parent => {
        val conditionallyImmutable = mutabilityComponent.classToCellCompleter.get(parent.typeSymbol.asInstanceOf[mutabilityComponent.global.Symbol]) match {
          case Some(cellCompleter) => cellCompleter.cell.getResult == ConditionallyImmutable
          case _ => false
        }
        if (conditionallyImmutable) {
          parent.typeArgs.foreach(typeArgument => {
            mutabilityComponent.classToCellCompleter.get(typeArgument.typeSymbol.asInstanceOf[mutabilityComponent.global.Symbol]) match {
              case Some(typeArgumentCellCompleter) =>
                if (typeArgumentCellCompleter.cell.getResult() == Mutable || typeArgumentCellCompleter.cell.getResult() == ShallowImmutable) {
                  println(s"Class inheritance used Mutable/Shallow on a ConditionallyImmutable. Klass: ${klass}, Mutable/Shallow: ${typeArgument.typeSymbol.fullName}, cond: ${parent.typeSymbol.fullName}")
                  val immutability = mutabilityComponent.classToCellCompleter.getOrElse(klass, null).cell.getResult()
                  println(s"Found a class '${klass}' (with immutability property ${immutability}) that extends a klass '${parent.typeSymbol.fullName}' (that is conditionally immutable) using a type argument with immutability property '${typeArgumentCellCompleter.cell.getResult()}' (${typeArgument.typeSymbol.fullName})")
                  if (klass.isModuleClass) {
                    immutability match {
                      case Immutable => immutableObjects += klass
                      case ShallowImmutable => shallowImmutableObjects += klass
                      case Mutable => mutableObjects += klass
                      case ConditionallyImmutable => conditionallyImmutableObjects += klass
                      case _ =>
                    }
                  } else if (klass.isAnonymousClass) {
                    immutability match {
                      case Immutable => immutableAnonymousClasses += klass
                      case ShallowImmutable => shallowImmutableAnonymousClasses += klass
                      case Mutable => mutableAnonymousClasses += klass
                      case ConditionallyImmutable => conditionallyImmutableAnonymousClasses += klass
                      case _ =>
                    }
                  } else {
                    if (klass hasFlag Flag.TRAIT) {
                      immutability match {
                        case Immutable => immutableTraits += klass
                        case ShallowImmutable => shallowImmutableTraits += klass
                        case Mutable => mutableTraits += klass
                        case ConditionallyImmutable => conditionallyImmutableTraits += klass
                        case _ =>
                      }
                    } else if (klass hasFlag Flag.CASE) {
                      immutability match {
                        case Immutable => immutableCaseClasses += klass
                        case ShallowImmutable => shallowImmutableCaseClasses += klass
                        case Mutable => mutableCaseClasses += klass
                        case ConditionallyImmutable => conditionallyImmutableCaseClasses += klass
                        case _ =>
                      }
                    } else if (klass hasFlag Flag.ABSTRACT) {
                      immutability match {
                        case Immutable => immutableAbstractClasses += klass
                        case ShallowImmutable => shallowImmutableAbstractClasses += klass
                        case Mutable => mutableAbstractClasses += klass
                        case ConditionallyImmutable => conditionallyImmutableAbstractClasses += klass
                        case _ =>
                      }
                    } else {
                      immutability match {
                        case Immutable => immutableClasses += klass
                        case ShallowImmutable => shallowImmutableClasses += klass
                        case Mutable => mutableClasses += klass
                        case ConditionallyImmutable => conditionallyImmutableClasses += klass
                        case _ =>
                      }
                    }
                  }
                }
              case _ =>
            }
          })
        }
      })
    })
    println("## Classes that extend conditionally immutable with shallow/mutable argument")
    println(s"Type that extend ⬇️️ | Type's immutability ➡️️ \t Mutable \t Shallow immutable \t Deeply immutable \t Conditionally deeply immutable")
    println(s"Classes \t ${mutableClasses.size} \t ${shallowImmutableClasses.size} \t ${immutableClasses.size} \t ${conditionallyImmutableClasses.size}")
    println(s"Case Classes \t ${mutableCaseClasses.size} \t ${shallowImmutableCaseClasses.size} \t ${immutableCaseClasses.size} \t ${conditionallyImmutableCaseClasses.size}")
    println(s"Abstract Classes \t ${mutableAbstractClasses.size} \t ${shallowImmutableAbstractClasses.size} \t ${immutableAbstractClasses.size} \t ${conditionallyImmutableAbstractClasses.size}")
    println(s"Traits \t ${mutableTraits.size} \t ${shallowImmutableTraits.size} \t ${immutableTraits.size} \t ${conditionallyImmutableTraits.size}")
    println(s"Objects \t ${mutableObjects.size} \t ${shallowImmutableObjects.size} \t ${immutableObjects.size} \t ${conditionallyImmutableObjects.size}")
    println(s"Anonymous Classes \t ${mutableAnonymousClasses.size} \t ${shallowImmutableAnonymousClasses.size} \t ${immutableAnonymousClasses.size} \t ${conditionallyImmutableAnonymousClasses.size}")
  }

  def conditionallyImmutableField() = {
    var mutableObjects: Set[mutabilityComponent.global.Symbol] = Set()
    var mutableTraits: Set[mutabilityComponent.global.Symbol] = Set()
    var mutableCaseClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var mutableAbstractClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var mutableClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var mutableAnonymousClasses: Set[mutabilityComponent.global.Symbol] = Set()

    var shallowImmutableObjects: Set[mutabilityComponent.global.Symbol] = Set()
    var shallowImmutableTraits: Set[mutabilityComponent.global.Symbol] = Set()
    var shallowImmutableCaseClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var shallowImmutableAbstractClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var shallowImmutableClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var shallowImmutableAnonymousClasses: Set[mutabilityComponent.global.Symbol] = Set()

    var immutableObjects: Set[mutabilityComponent.global.Symbol] = Set()
    var immutableTraits: Set[mutabilityComponent.global.Symbol] = Set()
    var immutableCaseClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var immutableAbstractClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var immutableClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var immutableAnonymousClasses: Set[mutabilityComponent.global.Symbol] = Set()

    var conditionallyImmutableObjects: Set[mutabilityComponent.global.Symbol] = Set()
    var conditionallyImmutableTraits: Set[mutabilityComponent.global.Symbol] = Set()
    var conditionallyImmutableCaseClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var conditionallyImmutableAbstractClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var conditionallyImmutableClasses: Set[mutabilityComponent.global.Symbol] = Set()
    var conditionallyImmutableAnonymousClasses: Set[mutabilityComponent.global.Symbol] = Set()

    scanComponent.classesThatHaveFieldsWithTypeArguments.foreach(x => {
      val klass = x._1.asInstanceOf[mutabilityComponent.global.Symbol]
      val fields = x._2
      fields.foreach(field => {
        field.typeArgs.foreach(typeArgument => {
          val conditionallyImmutable = mutabilityComponent.classToCellCompleter.get(field.typeSymbol.asInstanceOf[mutabilityComponent.global.Symbol]) match {
            case Some(cellCompleter) => cellCompleter.cell.getResult == ConditionallyImmutable
            case _ => false
          }
          if (conditionallyImmutable) {
            mutabilityComponent.classToCellCompleter.get(typeArgument.typeSymbol.asInstanceOf[mutabilityComponent.global.Symbol]) match {
              case Some(typeArgumentCellCompleter) =>
                if (typeArgumentCellCompleter.cell.getResult() == Mutable || typeArgumentCellCompleter.cell.getResult() == ShallowImmutable) {
                  val immutability = mutabilityComponent.classToCellCompleter.getOrElse(klass.asInstanceOf[mutabilityComponent.global.Symbol], null).cell.getResult()
                  println(s"Found a class '${klass}' (with immutability property ${immutability}) that had a field '${field}' that used a type with immutability property '${typeArgumentCellCompleter.cell.getResult()}' (${typeArgument.typeSymbol.fullName}) on a conditionally immutable class (${field.typeSymbol})")
                  if (klass.isModuleClass) {
                    immutability match {
                      case Immutable => immutableObjects += klass
                      case ShallowImmutable => shallowImmutableObjects += klass
                      case Mutable => mutableObjects += klass
                      case ConditionallyImmutable => conditionallyImmutableObjects += klass
                      case _ =>
                    }
                  } else if (klass.isAnonymousClass) {
                    immutability match {
                      case Immutable => immutableAnonymousClasses += klass
                      case ShallowImmutable => shallowImmutableAnonymousClasses += klass
                      case Mutable => mutableAnonymousClasses += klass
                      case ConditionallyImmutable => conditionallyImmutableAnonymousClasses += klass
                      case _ =>
                    }
                  } else {
                    if (klass hasFlag Flag.TRAIT) {
                      immutability match {
                        case Immutable => immutableTraits += klass
                        case ShallowImmutable => shallowImmutableTraits += klass
                        case Mutable => mutableTraits += klass
                        case ConditionallyImmutable => conditionallyImmutableTraits += klass
                        case _ =>
                      }
                    } else if (klass hasFlag Flag.CASE) {
                      immutability match {
                        case Immutable => immutableCaseClasses += klass
                        case ShallowImmutable => shallowImmutableCaseClasses += klass
                        case Mutable => mutableCaseClasses += klass
                        case ConditionallyImmutable => conditionallyImmutableCaseClasses += klass
                        case _ =>
                      }
                    } else if (klass hasFlag Flag.ABSTRACT) {
                      immutability match {
                        case Immutable => immutableAbstractClasses += klass
                        case ShallowImmutable => shallowImmutableAbstractClasses += klass
                        case Mutable => mutableAbstractClasses += klass
                        case ConditionallyImmutable => conditionallyImmutableAbstractClasses += klass
                        case _ =>
                      }
                    } else {
                      immutability match {
                        case Immutable => immutableClasses += klass
                        case ShallowImmutable => shallowImmutableClasses += klass
                        case Mutable => mutableClasses += klass
                        case ConditionallyImmutable => conditionallyImmutableClasses += klass
                        case _ =>
                      }
                    }
                  }
                }
              case _ =>
            }
          }
        })
      })
    })

    println("## Classes that has a field that use a conditionally immutable with shallow/mutable argument")
    println(s"Type with field ⬇️️ | Type's immutability ➡️️ \t Mutable \t Shallow immutable \t Deeply immutable \t Conditionally deeply immutable")
    println(s"Classes \t ${mutableClasses.size} \t ${shallowImmutableClasses.size} \t ${immutableClasses.size} \t ${conditionallyImmutableClasses.size}")
    println(s"Case Classes \t ${mutableCaseClasses.size} \t ${shallowImmutableCaseClasses.size} \t ${immutableCaseClasses.size} \t ${conditionallyImmutableCaseClasses.size}")
    println(s"Abstract Classes \t ${mutableAbstractClasses.size} \t ${shallowImmutableAbstractClasses.size} \t ${immutableAbstractClasses.size} \t ${conditionallyImmutableAbstractClasses.size}")
    println(s"Traits \t ${mutableTraits.size} \t ${shallowImmutableTraits.size} \t ${immutableTraits.size} \t ${conditionallyImmutableTraits.size}")
    println(s"Objects \t ${mutableObjects.size} \t ${shallowImmutableObjects.size} \t ${immutableObjects.size} \t ${conditionallyImmutableObjects.size}")
    println(s"Anonymous Classes \t ${mutableAnonymousClasses.size} \t ${shallowImmutableAnonymousClasses.size} \t ${immutableAnonymousClasses.size} \t ${conditionallyImmutableAnonymousClasses.size}")
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
      "Objects"
    } else if (sym.isAnonymousClass) {
      "Anonymous classes"
    } else {
      if (sym hasFlag Flag.TRAIT) {
        "Traits"
      } else if (sym hasFlag Flag.CASE) {
        "Case classes"
      } else if (sym hasFlag Flag.ABSTRACT) {
        "Abstract classes"
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
