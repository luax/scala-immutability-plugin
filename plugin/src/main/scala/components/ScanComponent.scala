package components

import cell.CellCompleter
import helpers.Utils
import immutability._

import scala.tools.nsc.plugins.{PluginComponent => NscPluginComponent}
import scala.tools.nsc.{Global, Phase}

class ScanComponent(val global: Global, val phaseName: String, val runsAfterPhase: String) extends NscPluginComponent {

  import global._

  import reflect.internal.Flags._

  override val runsAfter = List(runsAfterPhase)

  var compilationUnitToCellCompleters = Map[CompilationUnit, Map[Symbol, CellCompleter[ImmutabilityKey.type, Immutability]]]()

  var classesWithVar: Set[Symbol] = Set()
  var classesWithVal: Set[Symbol] = Set()
  var classes: Set[Symbol] = Set()
  var traits: Set[Symbol] = Set()
  var objects: Set[Symbol] = Set()
  var templates: Set[Symbol] = Set()

  var classesWithoutCellCompleter: Set[Symbol] = Set()
  var assignmentWithoutCellCompleter: Set[Symbol] = Set()

  def numOfClasses = classes.size

  def numOfTraits = traits.size

  def numOfObjects = objects.size

  def numOfTempls = templates.size

  class UnitContentTraverser extends Traverser {
    var classToCellCompleter: Map[Symbol, CellCompleter[ImmutabilityKey.type, Immutability]] = Map()

    def ensureCellCompleter(symbol: Symbol): Unit = {
      if (classToCellCompleter.get(symbol) != null) {
        val completer = CellCompleter[ImmutabilityKey.type, Immutability](Utils.getPool, ImmutabilityKey)
        classToCellCompleter += (symbol -> completer)
      }
    }

    def countClassDef(symbol: Symbol, mods: Modifiers): Unit = {
      if (symbol.isModuleClass) {
        objects += symbol
      } else {
        if (mods hasFlag Flag.TRAIT) {
          traits += symbol
        } else {
          classes += symbol
        }
      }
    }

    def countClassWith(klass: Symbol, mods: Modifiers): Unit = {
      // TODO lazy val
      if (mods.hasFlag(MUTABLE)) {
        classesWithVar += klass
      } else if (!mods.hasFlag(SYNTHETIC)) {
        classesWithVal += klass
      }
    }

    override def traverse(tree: Tree): Unit = tree match {
      case cls@ClassDef(mods, name, tparams, impl) =>
        val symbol = cls.symbol
        countClassDef(symbol, mods)
        ensureCellCompleter(symbol)
        traverse(impl)

      case templ@Template(parents, self, body) =>
        val cls = templ.symbol.owner
        templates += cls
        body.foreach(t => traverse(t))

      case vd@ValDef(mods, name, tpt, rhs) =>
        if (vd.symbol.owner.isClass) {
          val klass = vd.symbol.owner
          countClassWith(klass, mods)
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
      val unitContentTraverser = new UnitContentTraverser()
      unitContentTraverser.traverse(unit.body)
      if (Utils.isScalaTest) {
        // If in test, overwrite map for each compilation unit
        compilationUnitToCellCompleters = Map(unit -> unitContentTraverser.classToCellCompleter)
      } else {
        compilationUnitToCellCompleters += unit -> unitContentTraverser.classToCellCompleter
      }
    }
  }

  override def newPhase(prev: Phase): StdPhase = new FirstPhase(prev)
}
