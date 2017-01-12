package components

import cell.CellCompleter
import helpers.Utils
import immutability._

import scala.collection.mutable._
import scala.tools.nsc.plugins.{PluginComponent => NscPluginComponent}
import scala.tools.nsc.{Global, Phase}

class ScanComponent(val global: Global, val phaseName: String, val runsAfterPhase: String) extends NscPluginComponent {

  import global._

  import reflect.internal.Flags._

  override val runsAfter = List(runsAfterPhase)

  var compilationUnitToCellCompleters: Map[CompilationUnit, Map[Symbol, CellCompleter[ImmutabilityKey.type, Immutability]]] = _
  var classesWithVar: Set[Symbol] = _
  var classesWithVal: Set[Symbol] = _
  var classesWithLazyVals: Set[Symbol] = _
  var caseClasses: Set[Symbol] = _
  var abstractClasses: Set[Symbol] = _
  var classes: Set[Symbol] = _
  var traits: Set[Symbol] = _
  var objects: Set[Symbol] = _
  var templates: Set[Symbol] = _
  var classesWithoutCellCompleter: Set[Symbol] = _
  var assignmentWithoutCellCompleter: Set[Symbol] = _
  var classesThatExtendWithTypeArguments: Map[Symbol, Set[Type]] = _
  var classesThatHaveFieldsWithTypeArguments: Map[Symbol, Set[Type]] = _

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
    classesThatExtendWithTypeArguments = Map[Symbol, Set[Type]]()
    classesThatHaveFieldsWithTypeArguments = Map[Symbol, Set[Type]]()
  }

  initializeFields()

  override def newPhase(prev: Phase): StdPhase = new FirstPhase(prev)

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
      if (mods.hasFlag(LAZY)) {
        classesWithLazyVals += klass
      } else if (mods.hasFlag(MUTABLE)) {
        classesWithVar += klass
      } else if (!compilerGenerated(mods)) {
        classesWithVal += klass
      }
    }

    override def traverse(tree: Tree): Unit = tree match {
      case cls@ClassDef(mods, name, tparams, impl) =>
        val symbol = cls.symbol
        if (!compilerGenerated(mods)) {
          // && !cls.symbol.isAnonymousClass) {
          // TODO: Anonymous class
          countClassDef(symbol, mods)
          ensureCellCompleter(symbol)

          // Check if this class extends  a class with any type argument
          symbol.tpe.parents.foreach(parent => {
            if (parent.typeArgs.nonEmpty) {
              classesThatExtendWithTypeArguments.get(symbol) match {
                case Some(set) =>
                  set += parent
                case _ =>
                  classesThatExtendWithTypeArguments += (symbol -> Set(parent))
              }
            }
          })
        }
        traverse(impl)

      case templ@Template(parents, self, body) =>
        val cls = templ.symbol.owner
        templates += cls
        body.foreach(t => traverse(t))

      case vd@ValDef(mods, name, tpt, rhs) =>
        if (!compilerGenerated(mods)) {
          val klass = vd.symbol.owner
          if (klass.isClass) {
            countClassWith(klass, mods)
            // If field has any type args
            if (rhs.tpe.typeArgs.nonEmpty) {
              classesThatHaveFieldsWithTypeArguments.get(klass) match {
                case Some(set) =>
                  set += rhs.tpe
                case _ =>
                  classesThatHaveFieldsWithTypeArguments += (klass -> Set(rhs.tpe))
              }
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
      } else {
        Utils.log(s"Scanning compilation unit: '${unit}'")
      }
      val unitContentTraverser = new UnitContentTraverser()
      unitContentTraverser.traverse(unit.body)
      compilationUnitToCellCompleters += unit -> unitContentTraverser.classToCellCompleter
    }
  }
}
