package components

import cell.{CellCompleter, FalsePred, WhenNextComplete}
import helpers.Utils
import immutability._

import scala.tools.nsc.plugins.{PluginComponent => NscPluginComponent}
import scala.tools.nsc.{Global, Phase}
import scala.util.{Failure, Success}

class MutabilityComponent(val global: Global, val phaseName: String, val runsAfterPhase: String, val scanComponent: ScanComponent) extends NscPluginComponent {

  import global._

  import reflect.internal.Flags._

  override val runsAfter = List(runsAfterPhase)

  var classToCellCompleter: Map[Symbol, CellCompleter[ImmutabilityKey.type, Immutability]] = Map()
  var classesWithoutCellCompleter: Set[Symbol] = Set()
  var assignmentWithoutCellCompleter: Set[Symbol] = Set()

  class MutabilityTraverser() extends Traverser {


    def forEachParentOfClass (klass: Symbol): Unit = {
      for (parent <- klass.tpe.parents) {

        val parentCompleter = classToCellCompleter.getOrElse(parent.typeSymbol, null)
        if (parentCompleter != null) {
          val klassCompleter = classToCellCompleter.getOrElse(klass, null)
          if (klassCompleter != null) {
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
          } else {
            classesWithoutCellCompleter += klass
            Utils.log(s"Did not find a cell completer for this CLASS: $klass")
            // TODO:
            // String, Object is immutable etc

          }
        } else {
          Utils.log(s"Did not find a cell completer for PARENT, c: $klass, parent: $parent.typeSymbol")
        }
      }
    }


    override def traverse(tree: Tree): Unit = tree match {
      case cls@ClassDef(mods, name, tparams, impl) =>
        val klass = cls.symbol
        Utils.log(s"Inspecting class: $klass")
        forEachParentOfClass(klass)
        traverse(impl)

      case foo@ModuleDef(mods: Modifiers, name: TermName, impl: Template) =>
        println(foo)

      case vd@ValDef(mods, name, tpt, rhs) =>
        val klass = vd.symbol.owner
        Utils.log(s"Inspecting vd: $vd")

        if (klass.isClass) {
          // The owner of the value defintion is a class
          val klassCompleter = classToCellCompleter.getOrElse(klass, null)
          if (klassCompleter != null) {
            if (mods.hasFlag(MUTABLE)) {
              // It's a mutable value, e.g. "var x"
              klassCompleter.putFinal(Mutable)
            } else if (!mods.hasFlag(SYNTHETIC)) {
              // It's an immutable values, e.g. "val x"
              // Investigate the "right-hand" side of the assignment
              val assignedType = rhs.tpe // E.g. new String("foo")
              val assignedTypeSymbol = assignedType.typeSymbol // E.g. "class String"
              val assignedTypeCompleter = classToCellCompleter.getOrElse(assignedTypeSymbol, null)
              if (assignedTypeCompleter != null) {
                // Check the cell of the right-hand side's class, e.g. "Foo" in "val x = new Foo"
                if (assignedTypeCompleter.cell.isComplete) {
                  // The cell of "Foo" was complete, set owner of vd cell to the same mutability
                  klassCompleter.putNext(assignedTypeCompleter.cell.getResult)
                } else {
                  // Cell not completed yet, when assigned a mutability also set owner of vd cell
                  // to the same mutability
                  assignedTypeCompleter.cell.whenNext(klassCompleter.cell, (x: Immutability) => {
                    if (x == Mutable) {
                      WhenNextComplete
                    } else {
                      FalsePred
                    }
                  }, None)
                }
              } else {
                Utils.log(s"Did not find a cell completer for this ASSIGNMENT, c: $klass, field: $vd")
                assignmentWithoutCellCompleter += assignedTypeSymbol
              }
            }
          } else {
            Utils.log(s"Did not find a cell completer for this CLASS, c: $klass, field: $vd")
            classesWithoutCellCompleter += klass
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
