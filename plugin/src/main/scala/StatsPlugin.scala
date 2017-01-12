import components.{MutabilityComponent, ReporterComponent, ScanComponent}
import helpers.Utils

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}

class StatsPlugin(val global: Global) extends NscPlugin {
  /* Info */
  val name = "stats-plugin"
  val description = "A one-line description of the plugin"

  Utils.log(s"Running plugin: $name")

  /* The phases */
  val runAfterPhase = "refchecks"
  val phaseOne = "phase name 1"
  val phaseTwo = "phase name 2"
  val phaseThree = "phase name 3"

  /* Components */
  var scanComponent = new ScanComponent(global, phaseOne, runAfterPhase)
  val mutabilityPluginComponent = new MutabilityComponent(global, phaseTwo, phaseOne, scanComponent)
  val reporterPluginComponent = new ReporterComponent(global, phaseThree, phaseTwo, scanComponent, mutabilityPluginComponent)
  val components = List[NscPluginComponent](scanComponent, mutabilityPluginComponent, reporterPluginComponent)

}
