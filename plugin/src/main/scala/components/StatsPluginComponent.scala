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
  val mutabilityTraverser = new MutabilityTraverser

  /*
   * Helper functions
   */
  def notifyTest(pos: Position, klass: Symbol, message: String): Unit = {
    if (Utils.isScalaTest) {
      val testMessage = Utils.getCurrentTestMessage
      val errorMessage = Utils.mutabilityMessage(klass.name.toString, message)
      if (testMessage == errorMessage) {
        println(s"Reporting '$errorMessage'")
        // False negative error to notify test that it was successful
        global.reporter.error(pos, errorMessage)
      } else {
        println(s"Tried to report '$errorMessage' but want '$testMessage'")
      }
    }
  }

  def numOfClassesWithVar = countTraverser.classesWithVar.size

  def numOfClassesWithVal = countTraverser.classesWithVal.size

  def numOfClasses = countTraverser.classes.size

  def numOfTraits = countTraverser.traits.size

  def numOfObjects = countTraverser.objects.size

  def numOfTempls = countTraverser.templates.size

  /*
   * Traversers
   */
  class CountTraverser() extends Traverser {
    var classesWithVar: Set[Symbol] = Set()
    var classesWithVal: Set[Symbol] = Set()
    var classes: Set[Symbol] = Set()
    var traits: Set[Symbol] = Set()
    var objects: Set[Symbol] = Set()
    var templates: Set[Symbol] = Set()

    // TODO: Might use these:
    var classesWithPrivateVar: Set[Symbol] = Set()
    var classesWithPrivateVal: Set[Symbol] = Set()
    var classesWithPrivateThisVar: Set[Symbol] = Set()
    var classesWithPrivateThisVal: Set[Symbol] = Set()

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

      case vd@ValDef(mods, name, tpt, rhs) =>
        if (vd.symbol.owner.isClass) {
          val klass = vd.symbol.owner;
          // TODO lazy val

          if (mods.hasFlag(MUTABLE)) {
            classesWithVar += klass
          } else if (!mods.hasFlag(SYNTHETIC)) {
            classesWithVal += klass
          }
        }
        traverse(rhs)
      case _ =>
        super.traverse(tree)
    }

  }

  class MutabilityTraverser() extends Traverser {

    def parentIsMutable(tpe: Type): Boolean = {
      for (parent <- tpe.parents) {
        if (countTraverser.classesWithVar.contains(parent.typeSymbol)) {
          return true
        } else {
          return parentIsMutable(parent)
        }
      }
      return false
    }

    override def traverse(tree: Tree): Unit = tree match {
      case cls@ClassDef(mods, name, tparams, impl) =>
        val klass = cls.symbol
        println(s"Inspecting class: $klass")
        // TODO: Any weird parent class that can this check?
        if (parentIsMutable(klass.tpe)) {
          notifyTest(cls.pos, klass, Utils.IsMutable)
        }
        traverse(impl)

      case vd@ValDef(mods, name, tpt, rhs) =>
        val klass = vd.symbol.owner
        if (klass.isClass) {
          // Owner of value definition is a class
          if (mods.hasFlag(MUTABLE)) {
            // It's a mutable value, e.g. "var x"
            notifyTest(vd.pos, klass, Utils.IsMutable)
          } else if (!mods.hasFlag(SYNTHETIC)) {
            // It's an immutable values, e.g. "val x"
            val assignedType = rhs.tpe
            if (countTraverser.classesWithVar.contains(assignedType.typeSymbol)) {
              // Shallow immutable if, e.g. "val x = new Foo"
              // TODO: Need to check primitive type mutability
              notifyTest(vd.pos, klass, Utils.IsShallowImmutable);
            }
            if (countTraverser.classesWithVal.contains(klass)) {
              // TODO: Something I guess...
            }
          }
        }
        traverse(rhs)
      case _ =>
        super.traverse(tree)
    }

  }

  /*
   * Phase
   */
  class FirstPhase(prev: Phase) extends StdPhase(prev) {
    override def apply(unit: CompilationUnit): Unit = {
      println("Starting phase: " + phase)
      countTraverser.traverse(unit.body)
      mutabilityTraverser.traverse(unit.body)
      println("Done with phase: " + phase)
    }
  }

  override def newPhase(prev: Phase): StdPhase = new FirstPhase(prev)
}
