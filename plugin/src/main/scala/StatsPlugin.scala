import components.{MutabilityComponent, ReporterComponent, ScanComponent}
import helpers.Utils

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}

class StatsPlugin(val global: Global) extends NscPlugin {
  val name = "stats-plugin"
  val description = "TODO plugin"

  val phaseOne = "phase name 1"
  val phaseTwo = "phase name 2"
  val phaseThree = "phase name 3"

  Utils.newPool // TODO:

  println("Starting plugin!")

  var scanComponent = new ScanComponent(global, phaseOne, "refchecks")
  val mutabilityPluginComponent = new MutabilityComponent(global, phaseTwo, phaseOne, scanComponent)
  val reporterPluginComponent = new ReporterComponent(global, phaseThree, phaseTwo, scanComponent, mutabilityPluginComponent)

  val components = List[NscPluginComponent](scanComponent, mutabilityPluginComponent, reporterPluginComponent)
}
