import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}
import scala.collection.{mutable, immutable}

class ReporterPluginComponent(val global: Global, val statsPluginComponent: StatsPluginComponent) extends NscPluginComponent {
  import global.{log => _, _}

  override val runsAfter = List("stats")
  val phaseName = "stats-reporter"

  var hasRun = false

  override def newPhase(prev: Phase): StdPhase =
    new StdPhase(prev) {
      override def apply(unit: CompilationUnit): Unit =
        if (!hasRun) {
          hasRun = true
          reporter.echo("Plugin ran successfully")
          reporter.echo("#classes analyzed: " + statsPluginComponent.analyzedClasses.size)
          reporter.echo("#traits analyzed: " + statsPluginComponent.analyzedTraits.size)
          reporter.echo("#objects analyzed: " + statsPluginComponent.analyzedObjects.size)
          reporter.echo("#templs analyzed: " + statsPluginComponent.analyzedTempls.size)

          reporter.echo(s"#objects mutable: ${statsPluginComponent.mutableObjects.size}")
          statsPluginComponent.mutableObjects.foreach { sym =>
            reporter.echo(s"object ${sym.name}")
          }
        }
    }
}
