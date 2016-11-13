import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}

class DivByZero(val global: Global) extends NscPlugin {
  import global._

  val name = "divbyzero"
  val description = "checks for division by zero"
  val components = List[NscPluginComponent](Component)

  private object Component extends NscPluginComponent {
    val global: DivByZero.this.global.type = DivByZero.this.global
    // Using the Scala Compiler 2.8.x the runsAfter should be written as below
    val runsAfter = List[String]("refchecks")
    val phaseName = DivByZero.this.name
    def newPhase(_prev: Phase) = new DivByZeroPhase(_prev)

    class DivByZeroPhase(prev: Phase) extends StdPhase(prev) {
      override def name = DivByZero.this.name
      def apply(unit: CompilationUnit) {
        for ( tree @ Apply(Select(rcvr, nme.DIV), List(Literal(Constant(0)))) <- unit.body;
             if rcvr.tpe <:< definitions.IntClass.tpe)
          {
            global.reporter.error(tree.pos, "definitely division by zero")
          }
      }
    }
  }
}
