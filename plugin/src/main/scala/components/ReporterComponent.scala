package components

import helpers.Utils
import immutability.{Immutable, Mutable}

import scala.collection.mutable.ListBuffer
import scala.tools.nsc._
import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}

class ReporterComponent(val global: Global, val phaseName: String, val runsAfterPhase: String, val scanComponent: ScanComponent, val mutabilityPluginComponent: MutabilityComponent) extends NscPluginComponent {

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
      return true
    } else {
      Utils.log(s"Tried to report '$errorMessage' but want '$testMessage'")
      return false
    }
  }

  def notifyTest(): Unit = {
    for ((klass, v) <- mutabilityPluginComponent.classToCellCompleter) {
      val classSymbol = klass.asInstanceOf[Symbol]
      v.cell.getResult match {
        case Mutable => {
          if (notifyTest(classSymbol.pos, classSymbol, Utils.IsMutable)) {
            return // Break loop
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
        notifyTest()
        // For testing
        Utils.getPool.shutdown()
        Utils.newPool
      }
      if (!hasRun) {
        hasRun = true

        // TODO:
        // pool.whileQuiescentResolveDefault
        Utils.getPool.shutdown()

        reporter.echo("Plugin ran successfully")
        reporter.echo("#classes found: " + scanComponent.numOfClasses)
        reporter.echo("#traits found: " + scanComponent.numOfTraits)
        reporter.echo("#objects found: " + scanComponent.numOfObjects)
        reporter.echo("#templs found: " + scanComponent.numOfTempls)
        reporter.echo("-")
        reporter.echo("#classes with var: " + scanComponent.numOfClassesWithVar)
        reporter.echo("#classes with val: " + scanComponent.numOfClassesWithVal)
        reporter.echo("-")

        var mutables = new ListBuffer[Symbol]()
        var immutables = new ListBuffer[Symbol]()
        for ((klass, v) <- mutabilityPluginComponent.classToCellCompleter) {
          val immutability = v.cell.getResult
          if (immutability == Mutable) {
            mutables += klass.asInstanceOf[Symbol]
          } else if (immutability == Immutable) {
            immutables += klass.asInstanceOf[Symbol]
          }
        }
        reporter.echo("#mutable classes: " + mutables.size)
        reporter.echo(mutables.foldLeft("") { (r, s) => r + "[" + s + "]" + " " })

        reporter.echo("#immutable classes: " + immutables.size)
        reporter.echo(immutables.foldLeft("") { (r, s) => r + "[" + s + "]" + " " })
        reporter.echo("Reporter phase")
      }
    }
  }

  override def newPhase(prev: Phase): StdPhase = new NewPhase(prev)
}
