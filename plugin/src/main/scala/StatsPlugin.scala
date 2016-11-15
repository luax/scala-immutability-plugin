import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}
import scala.collection.{mutable, immutable}

class StatsPlugin(val global: Global) extends NscPlugin {

  import global.{log => _, _}

  val name = "stats-plugin"
  val description = "TODO plugin"
  val components = List[NscPluginComponent](PluginComponent, ReporterComponent)

  def log(msg: => String): Unit = {
    println(msg)
  }

  object PluginComponent extends NscPluginComponent {
    val global = StatsPlugin.this.global

    import global.{log => _, _}
    import reflect.internal.Flags._

    override val runsAfter = List("refchecks")
    val phaseName = "stats"

    var analyzedClasses: Set[Symbol] = Set()
    var analyzedTraits: Set[Symbol] = Set()
    var analyzedObjects: Set[Symbol] = Set()
    var analyzedTempls: Set[Symbol] = Set()

    def addClass(sym: Symbol): Unit =
      analyzedClasses += sym

    def addTrait(sym: Symbol): Unit =
      analyzedTraits += sym

    def addObject(sym: Symbol): Unit =
      analyzedObjects += sym

    def addTempl(sym: Symbol): Unit =
      analyzedTempls += sym

    var mutableObjects: Set[Symbol] = Set()

    def addMutableObject(sym: Symbol): Unit =
      mutableObjects += sym

    def isSetterOfObject(method: Symbol): Boolean =
      method.owner.isModuleClass && method.isSetter

    class TreeTraverser(unit: CompilationUnit) extends Traverser {
      var insecureMethods: Set[Symbol] = Set()
      var currentMethods: List[Symbol] = List()

      override def traverse(tree: Tree): Unit = tree match {

        case cls@ClassDef(mods, name, tparams, impl) =>
          log(StringContext("checking class ", "").s(name))
          log(StringContext("sym.isClass: ", "").s(cls.symbol.isClass))
          log(StringContext("sym.isModuleClass: ", "").s(cls.symbol.isModuleClass))
          if (cls.symbol.isModuleClass) {
            addObject(cls.symbol)
          } else {
            if (mods hasFlag TRAIT) {
              addTrait(cls.symbol)
            } else {
              addClass(cls.symbol)
            }
          }
          traverse(impl)
        case templ@Template(parents, self, body) =>
          val cls = templ.symbol.owner
          addTempl(cls)
          body.foreach(t => traverse(t))

        case vd@ValDef(mods, name, tpt, rhs) =>
          // if vd is a var and owner is object -> keep track of object!


          if (mods.hasFlag(MUTABLE) && vd.symbol.owner.isModuleClass) {
            addMutableObject(vd.symbol.owner)
          }
          traverse(rhs)

        case methodDef@DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
          log(StringContext("checking method definition ", "").s(methodDef.symbol.name))
          log(s"raw:\n${showRaw(methodDef)}")

          currentMethods = methodDef.symbol :: currentMethods
          traverse(rhs)
          currentMethods = currentMethods.tail

        case app@Apply(fun, args) =>
          // problem pattern 1: fun is setter of an object
          log(s"checking apply of ${fun.symbol.name}")
          log(s"setter of object: ${isSetterOfObject(fun.symbol)}")

          traverse(fun)
          args.foreach { arg => traverse(arg) }

        case unhandled =>
          log(s"unhandled tree $tree")
          log(s"raw:\n${showRaw(tree)}")
          super.traverse(tree)
      }
    }

    class FirstPhase(prev: Phase) extends StdPhase(prev) {
      override def apply(unit: CompilationUnit): Unit = {
        val t = new TreeTraverser(unit)
        t.traverse(unit.body)
      }
    }

    override def newPhase(prev: Phase): StdPhase =
      new FirstPhase(prev)
  }

  object ReporterComponent extends NscPluginComponent {
    val global = StatsPlugin.this.global

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
            reporter.echo("#classes analyzed: " + PluginComponent.analyzedClasses.size)
            reporter.echo("#traits analyzed: " + PluginComponent.analyzedTraits.size)
            reporter.echo("#objects analyzed: " + PluginComponent.analyzedObjects.size)
            reporter.echo("#templs analyzed: " + PluginComponent.analyzedTempls.size)

            reporter.echo(s"#objects mutable: ${PluginComponent.mutableObjects.size}")
            PluginComponent.mutableObjects.foreach { sym =>
              reporter.echo(s"object ${sym.name}")
            }
          }
      }
  }

}
