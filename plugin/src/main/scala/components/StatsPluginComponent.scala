package components

import helpers.Utils

import scala.tools.nsc.plugins.{PluginComponent => NscPluginComponent}
import scala.tools.nsc.{Global, Phase}

class StatsPluginComponent(val global: Global) extends NscPluginComponent {

  import global._
  import reflect.internal.Flags._

  override val runsAfter = List("refchecks")
  val phaseName = "stats"

  val countTraverser = new CountTraverser
  val classTraverser = new ClassTraverser

  /*
   * Helper functions
   */
  def notifyTest(pos: Position, message: String): Unit = {
    if (Utils.isScalaTest) {
      // False negative error to notify test that it was successful
      global.reporter.error(pos, message)
    }
  }


  /*
   * Count occurrences
   */
  def numOfClasses = countTraverser.classes.size

  def numOfTraits = countTraverser.traits.size

  def numOfObjects = countTraverser.objects.size

  def numOfTempls = countTraverser.templates.size

  class CountTraverser() extends Traverser {
    var classes: Set[Symbol] = Set()
    var traits: Set[Symbol] = Set()
    var objects: Set[Symbol] = Set()
    var templates: Set[Symbol] = Set()

    override def traverse(tree: Tree): Unit = tree match {
      case cls@ClassDef(mods, name, tparams, impl) =>
        val symbol = cls.symbol
        if (cls.symbol.isModuleClass) {
          objects += symbol
        } else {
          if (mods hasFlag Flag.TRAIT) {
            traits += symbol
          } else {
            classes += symbol
          }
        }
        traverse(impl)
      case templ@Template(parents, self, body) =>
        val cls = templ.symbol.owner
        templates += cls
        body.foreach(t => traverse(t))
      case _ =>
        super.traverse(tree)
    }
  }

  /*
   * Mutable classes
   */
  def numOfClassesWithVar = classTraverser.classesWithVar.size

  def numOfClassesWithVal = classTraverser.classesWithVal.size

  class ClassTraverser() extends Traverser {
    var classesWithVar: Set[Symbol] = Set()
    var classesWithVal: Set[Symbol] = Set()

    // TODO
    var classesWithPrivateVar: Set[Symbol] = Set()
    var classesWithPrivateVal: Set[Symbol] = Set()
    var classesWithPrivateThisVar: Set[Symbol] = Set()
    var classesWithPrivateThisVal: Set[Symbol] = Set()

    override def traverse(tree: Tree): Unit = tree match {
      case vd@ValDef(mods, name, tpt, rhs) =>
        if (vd.symbol.owner.isClass) {
          if (mods.hasFlag(MUTABLE)) {
            classesWithVar += vd.symbol.owner
          } else if (!mods.hasFlag(SYNTHETIC)) {
            classesWithVal += vd.symbol.owner
          }
        }
        traverse(rhs)
      case _ =>
        super.traverse(tree)
    }
  }

  class FirstPhase(prev: Phase) extends StdPhase(prev) {
    override def apply(unit: CompilationUnit): Unit = {
      println("Starting phase: " + phase)

      println("Count traverse")
      countTraverser.traverse(unit.body)

      println("Mutable classes traverse")
      classTraverser.traverse(unit.body)

      notifyTest(unit.body.pos, "foo")

      println("Done with phase: " + phase)
    }
  }

  override def newPhase(prev: Phase): StdPhase = new FirstPhase(prev)
}
