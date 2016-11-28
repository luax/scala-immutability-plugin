package components

import cell.{CellCompleter, FalsePred, WhenNextComplete}
import helpers.Utils
import immutability._

import scala.tools.nsc.plugins.{PluginComponent => NscPluginComponent}
import scala.tools.nsc.{Global, Phase}

class MutabilityComponent(val global: Global, val phaseName: String, val runsAfterPhase: String, val scanComponent: ScanComponent) extends NscPluginComponent {

  import global._

  import reflect.internal.Flags._

  override val runsAfter = List(runsAfterPhase)

  var classToCellCompleter: Map[Symbol, CellCompleter[ImmutabilityKey.type, Immutability]] = Map()
  var classesWithoutCellCompleter: Set[Symbol] = Set()
  var assignmentWithoutCellCompleter: Set[Symbol] = Set()

  class MutabilityTraverser() extends Traverser {

    override def traverse(tree: Tree): Unit = tree match {
      case cls@ClassDef(mods, name, tparams, impl) =>
        val klass = cls.symbol

        for (parent <- klass.tpe.parents) {
          val parentCompleter = classToCellCompleter.getOrElse(parent.typeSymbol, null)
          // println(s"Parent: $parent")
          if (parentCompleter == null) {
            Utils.log(s"Did not find a cell completer for parent, c: $klass, parent: $parent.typeSymbol")
          } else {
            val klassCompleter = classToCellCompleter.getOrElse(klass, null)
            if (klassCompleter == null) {
              classesWithoutCellCompleter += klass
              Utils.log(s"Did not find a cell completer for this class: $klass")
              // TODO:
              // String, Object is immutable etc
              return
            } else {
              if (parentCompleter.cell.isComplete) {
                // If a superclass is mutable all subclasses are also mutable.
                // A subclass can never have a "better" mutability than it's superclass.
                klassCompleter.putFinal(parentCompleter.cell.getResult)
              } else {
                klassCompleter.cell.whenNext(parentCompleter.cell, (x: Immutability) => {
                  if (x == Mutable) {
                    WhenNextComplete
                  } else {
                    FalsePred
                  }
                }, None)
              }
            }
          }
        }

        traverse(impl)

      case vd@ValDef(mods, name, tpt, rhs) =>
        val klass = vd.symbol.owner
        if (klass.isClass) {
          val klassCompleter = classToCellCompleter.getOrElse(klass, null)
          if (klassCompleter == null) {
            Utils.log(s"Did not find a cell completer for this klass, c: $klass, field: $vd")
            classesWithoutCellCompleter += klass
            return
          }

          // Owner of value definition is a class
          if (mods.hasFlag(MUTABLE)) {
            // It's a mutable value, e.g. "var x"
            klassCompleter.putFinal(Mutable)
          } else if (!mods.hasFlag(SYNTHETIC)) {
            // It's an immutable values, e.g. "val x"
            val assignedType = rhs.tpe
            val assignedTypeSymbol = assignedType.typeSymbol
            val assignedTypeCompleter = classToCellCompleter.getOrElse(assignedTypeSymbol, null)
            if (assignedTypeCompleter == null) {
              Utils.log(s"Did not find a cell completer for this assignment, c: $klass, field: $vd")
              assignmentWithoutCellCompleter += assignedTypeSymbol
              return
            }
            if (assignedTypeCompleter.cell.isComplete) {
              klassCompleter.putNext(assignedTypeCompleter.cell.getResult)
            } else {
              assignedTypeCompleter.cell.whenNext(klassCompleter.cell, (x: Immutability) => {
                if (x == Mutable) {
                  WhenNextComplete
                } else {
                  FalsePred
                }
              }, None)
            }
          }
        } else {
          Utils.log(s"owner is not a class of field: $vd")
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

    var initialized = false

    def computeClassToCellCompleter() = {
      for ((compilationUnit, mapOfCellCompleters) <- scanComponent.compilationUnitToCellCompleters) {
        for ((klass, cellCompleter) <- mapOfCellCompleters) {
          classToCellCompleter += (klass.asInstanceOf[Symbol] -> cellCompleter) // TODO: double check if this cast works
        }
      }
    }

    override def apply(unit: CompilationUnit): Unit = {
      if (Utils.isScalaTest) {
        // If in in test, overwrite the map for each compilation unit
        classToCellCompleter = Map()
        computeClassToCellCompleter()
      } else if (!initialized) {
        // Do the computation once, the scan component has already
        // traversed all compilation units
        initialized = true
        computeClassToCellCompleter()
      }
      val mutabilityTraverser = new MutabilityTraverser()
      mutabilityTraverser.traverse(unit.body)
    }
  }

  override def newPhase(prev: Phase): StdPhase = new FirstPhase(prev)
}
