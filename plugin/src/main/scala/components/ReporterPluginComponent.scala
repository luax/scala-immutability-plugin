package components

import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}
import scala.tools.nsc.{Global, Phase}

class ReporterPluginComponent(val global: Global, val statsPluginComponent: StatsPluginComponent) extends NscPluginComponent {

  import global._

  override val runsAfter = List("stats")
  val phaseName = "stats-reporter"

  var hasRun = false

  override def newPhase(prev: Phase): StdPhase =
    new StdPhase(prev) {
      override def apply(unit: CompilationUnit): Unit =
        if (!hasRun) {
          hasRun = true
          reporter.echo("Plugin ran successfully")
          reporter.echo("#classes found: " + statsPluginComponent.numOfClasses)
          reporter.echo("#traits found: " + statsPluginComponent.numOfTraits)
          reporter.echo("#objects found: " + statsPluginComponent.numOfObjects)
          reporter.echo("#templs found: " + statsPluginComponent.numOfTempls)
          reporter.echo("-")
          reporter.echo("#classes with var: " + statsPluginComponent.numOfClassesWithVar)
          reporter.echo("#classes with val: " + statsPluginComponent.numOfClassesWithVal)
          reporter.echo("-")
          //reporter.echo(s"#objects mutable: ${statsPluginComponent.mutableObjects.size}")
          //statsPluginComponent.mutableObjects.foreach { sym =>
          //  reporter.echo(s"object ${sym.name}")
          //}
        }
    }
}
