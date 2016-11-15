import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}
import scala.collection.{mutable, immutable}

object Log {
  def log(msg: => String): Unit = {
    println(msg)
  }
}

class StatsPlugin(val global: Global) extends NscPlugin {
  val name = "stats-plugin"
  val description = "TODO plugin"
  var statsPluginComponent = new StatsPluginComponent(global)
  val reporterPluginComponent =  new ReporterPluginComponent(global, statsPluginComponent)
  val components = List[NscPluginComponent](statsPluginComponent, reporterPluginComponent)
}
