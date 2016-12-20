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

  var classesWithVar: Set[Symbol] = null
  var classesWithVal: Set[Symbol] = null
  var classesWithLazyVals: Set[Symbol] = null
  var caseClasses: Set[Symbol] = null
  var abstractClasses: Set[Symbol] = null
  var classes: Set[Symbol] = null
  var traits: Set[Symbol] = null
  var objects: Set[Symbol] = null
  var templates: Set[Symbol] = null
  var classesWithoutCellCompleter: Set[Symbol] = null
  var assignmentWithoutCellCompleter: Set[Symbol] = null

  def initializeFields(): Unit = {
    compilationUnitToCellCompleters = Map[CompilationUnit, Map[Symbol, CellCompleter[ImmutabilityKey.type, Immutability]]]()
    classesWithVar = Set()
    classesWithVal = Set()
    classesWithLazyVals = Set()
    caseClasses = Set()
    abstractClasses = Set()
    classes = Set()
    traits = Set()
    objects = Set()
    templates = Set()
    classesWithoutCellCompleter = Set()
    assignmentWithoutCellCompleter = Set()
  }

  initializeFields()

  class UnitContentTraverser extends Traverser {
    var classToCellCompleter: Map[Symbol, CellCompleter[ImmutabilityKey.type, Immutability]] = Map()

    def compilerGenerated(mods: Modifiers): Boolean = {
      // Symbol is compiler-generated
      mods.hasFlag(SYNTHETIC)
    }

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
        } else if (mods hasFlag Flag.CASE) {
          caseClasses += symbol
        } else if (mods.hasFlag(Flag.ABSTRACT)) {
          abstractClasses += symbol
        } else {
          classes += symbol
        }
      }
    }

    def countClassWith(klass: Symbol, mods: Modifiers): Unit = {
      // TODO lazy val
      if (mods.hasFlag(MUTABLE)) {
        classesWithVar += klass
      } else if (!compilerGenerated(mods)) {
        classesWithVal += klass
      }
    }

    override def traverse(tree: Tree): Unit = tree match {
      case cls@ClassDef(mods, name, tparams, impl) =>
        val symbol = cls.symbol
        if (!compilerGenerated(mods) && !cls.symbol.isAnonymousClass) {
          // TODO: Anonymous class
          // Symbol is not compiler-generated
          countClassDef(symbol, mods)
          ensureCellCompleter(symbol)
        }
        traverse(impl)

      case templ@Template(parents, self, body) =>
        val cls = templ.symbol.owner
        templates += cls
        body.foreach(t => traverse(t))

      case vd@ValDef(mods, name, tpt, rhs) =>
        if (!mods.hasFlag(SYNTHETIC)) {
          if (vd.symbol.owner.isClass) {
            val klass = vd.symbol.owner
            countClassWith(klass, mods)

            // Lazy
            if (mods.hasFlag(LAZY)) {
              classesWithLazyVals += klass
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
      if (Utils.isScalaTest) {
        // If in test, overwrite fields for each compilation unit
        initializeFields()
      }
      val unitContentTraverser = new UnitContentTraverser()
      unitContentTraverser.traverse(unit.body)
      compilationUnitToCellCompleters += unit -> unitContentTraverser.classToCellCompleter
    }
  }

  override def newPhase(prev: Phase): StdPhase = new FirstPhase(prev)
}
