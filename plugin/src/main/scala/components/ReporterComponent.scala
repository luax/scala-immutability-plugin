package components

import helpers.Utils
import immutability.{Immutable, MutabilityUnknown, Mutable, ShallowImmutable}

import scala.collection.mutable.ListBuffer
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
        case MutabilityUnknown => {
          if (notifyTest(classSymbol.pos, classSymbol, Utils.IsUnknownMutability)) {
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

        reporter.echo("Plugin ran successfully")
        reporter.echo("#classes found: " + scanComponent.numOfClasses)
        reporter.echo("#case classes found: " + scanComponent.numOfCaseClasses)
        reporter.echo("#abstract classes found: " + scanComponent.numOfAbstractClasses)
        reporter.echo("#traits found: " + scanComponent.numOfTraits)
        reporter.echo("#objects found: " + scanComponent.numOfObjects)
        reporter.echo("#templs found: " + scanComponent.numOfTempls)
        reporter.echo("-")
        reporter.echo("#classes with var: " + scanComponent.classesWithVar.size)
        reporter.echo(scanComponent.classesWithVar.foldLeft("") { (r, s) => r + "[" + s + "]" + " " })
        reporter.echo("#classes with val: " + scanComponent.classesWithVal.size)
        reporter.echo(scanComponent.classesWithVal.foldLeft("") { (r, s) => r + "[" + s + "]" + " " })
        reporter.echo("-")

        var mutableClasses = new ListBuffer[Symbol]()
        var mutableObjects = new ListBuffer[Symbol]()
        var mutableTraits = new ListBuffer[Symbol]()

        var immutableClasses = new ListBuffer[Symbol]()
        var immutableObjects = new ListBuffer[Symbol]()
        var immutableTraits = new ListBuffer[Symbol]()

        for ((klass, v) <- mutabilityComponent.classToCellCompleter) {
          val immutability = v.cell.getResult
          if (immutability == Mutable) {
            val k = klass.asInstanceOf[Symbol]
            if (k.isModuleClass) {
              mutableObjects += k
            } else {
              if (k hasFlag Flag.TRAIT) {
                mutableTraits += k
              } else {
                mutableClasses += k
              }
            }
          } else if (immutability == Immutable) {
            val k = klass.asInstanceOf[Symbol]
            if (k.isModuleClass) {
              immutableObjects += k
            } else {
              if (k hasFlag Flag.TRAIT) {
                immutableTraits += k
              } else {
                immutableClasses += k
              }
            }
          }
        }
        reporter.echo("#mutable classes: " + mutableClasses.size)
        reporter.echo(mutableClasses.foldLeft("") { (r, s) => r + "[" + s + "]" + " " })
        reporter.echo("#mutable objects: " + mutableObjects.size)
        reporter.echo(mutableObjects.foldLeft("") { (r, s) => r + "[" + s + "]" + " " })
        reporter.echo("#mutable traits: " + mutableTraits.size)
        reporter.echo(mutableTraits.foldLeft("") { (r, s) => r + "[" + s + "]" + " " })
        reporter.echo("-")
        reporter.echo("#immutable classes: " + immutableClasses.size)
        reporter.echo(immutableClasses.foldLeft("") { (r, s) => r + "[" + s + "]" + " " })
        reporter.echo("#immutable objects: " + immutableObjects.size)
        reporter.echo(immutableObjects.foldLeft("") { (r, s) => r + "[" + s + "]" + " " })
        reporter.echo("#immutable traits: " + immutableTraits.size)
        reporter.echo(immutableTraits.foldLeft("") { (r, s) => r + "[" + s + "]" + " " })
        reporter.echo("-")
        reporter.echo("#classes without cell completer: ")
        reporter.echo(mutabilityComponent.classesWithoutCellCompleter.foldLeft("") { (r, s) => r + "[" + s + "]" + " " })
        reporter.echo("#assignments without cell completer: ")
        reporter.echo(mutabilityComponent.assignmentWithoutCellCompleter.foldLeft("") { (r, s) => r + "[" + s + "]" + " " })
      }
    }
  }

  override def newPhase(prev: Phase): StdPhase = new NewPhase(prev)
}
