package components

import cell.{CellCompleter, FalsePred, WhenNext}
import helpers.Utils
import immutability._

import scala.tools.nsc.plugins.{PluginComponent => NscPluginComponent}
import scala.tools.nsc.{Global, Phase}

sealed trait ClassContext

case object ParentClassContext extends ClassContext

case object ValDefinitionContext extends ClassContext

case object ClassTypeParameterContext extends ClassContext

class MutabilityComponent(val global: Global, val phaseName: String, val runsAfterPhase: String, val scanComponent: ScanComponent) extends NscPluginComponent {

  import global._

  import reflect.internal.Flags._

  override val runsAfter = List(runsAfterPhase)

  var classToCellCompleter: Map[Symbol, CellCompleter[ImmutabilityKey.type, Immutability]] = Map()
  var cellCompleterToClass: Map[CellCompleter[ImmutabilityKey.type, Immutability], Symbol] = Map()

  var classesWithoutCellCompleter: Set[Symbol] = Set()
  var assignmentWithoutCellCompleter: Set[Symbol] = Set()

  var reasons = Map[Symbol, Symbol]()

  class MutabilityTraverser() extends Traverser {

    def addImmutabilityReason(gotImmutability: Symbol, fromImmutability: Symbol): Unit = {
      if (gotImmutability == null || fromImmutability == null) {
        println("addReason was null ", gotImmutability, fromImmutability)
        System.exit(0)
      }
      reasons += (gotImmutability -> fromImmutability)
    }

    def compilerGenerated(mods: Modifiers): Boolean = {
      // Symbol is compiler-generated
      mods.hasFlag(SYNTHETIC)
    }

    def putMutability(typeArgument: Type, klassCompleter: CellCompleter[ImmutabilityKey.type, Immutability], classContext: ClassContext): List[Immutability] = {
      val typeSymbol = typeArgument.typeSymbol // E.g. "class Mutable"
      val typeCellCompleter = classToCellCompleter.getOrElse(typeSymbol, null)
      if (typeCellCompleter == null) {
        // The type (e.g. Foo) did not have a cell completer and  could be an imported class from some unknown library
        // or some anonymous class/fn/
        // TODO: anonymous class/fn
        if (typeSymbol.isRefinementClass) {
          // Refinement class:
          // val foo = new A with B
          // Lookup A and B
          recursivePutMutability(typeArgument.parents, klassCompleter, ClassTypeParameterContext)
        } else {
          var mutability = KnownObjects.getMutability(typeSymbol.toString)
          if (mutability == MutabilityUnknown) {
            // If it is a known type such as "List", "scala.collection.mutable.ArrayBuffer" etc
            mutability = KnownObjects.getMutability(typeArgument.underlying.toString)
            if (mutability == MutabilityUnknown) {
              // It was not known by looking at the type string
              // assume that it's mutable
              println("Mutability of class was unknown", typeArgument.underlying)
              if (classContext == ParentClassContext) {
                // In the case, "class Child extends Parent { ... }" and "Parent" has "MutabilityUnknown"
                // assume "Child" to be "Mutable"
                mutability = Mutable
                addImmutabilityReason(cellCompleterToClass.getOrElse(klassCompleter, null), typeSymbol)
                klassCompleter.putFinal(mutability)
              } else if (classContext == ValDefinitionContext || classContext == ClassTypeParameterContext) {
                // In a val definition, set the class to be shallow immutable
                // since the val def points to a mutable (unknown type)
                mutability = ShallowImmutable
                addImmutabilityReason(cellCompleterToClass.getOrElse(klassCompleter, null), typeSymbol)
                klassCompleter.putNext(mutability)
              }
            }
          }
          klassCompleter.putNext(mutability)
          List(mutability)
        }
      } else {
        putMutability(typeCellCompleter, klassCompleter, classContext)
      }
    }

    def putMutability(childCellCompleter: CellCompleter[ImmutabilityKey.type, Immutability], ownerCellCompleter: CellCompleter[ImmutabilityKey.type, Immutability], classContext: ClassContext): List[Immutability] = {
      if (childCellCompleter.cell.isComplete) {
        var mutability: Immutability = null
        if (classContext == ParentClassContext) {
          // If a superclass is mutable all subclasses are also mutable.
          // A subclass can never have a "better" mutability than it's superclass.
          mutability = childCellCompleter.cell.getResult match {
            case Mutable => Mutable
            case ShallowImmutable => ShallowImmutable
            case _ => Immutable
          }
        } else if (classContext == ValDefinitionContext || classContext == ClassTypeParameterContext) {
          // The cell of "Foo" was complete, set owner of vd cell to the same mutability
          // If it complete it is either Mutable or ShallowImmutable.
          mutability = childCellCompleter.cell.getResult match {
            case Mutable => ShallowImmutable
            case ShallowImmutable => ShallowImmutable
            case _ => Immutable
          }
        }
        if (mutability == Mutable) {
          ownerCellCompleter.putFinal(mutability)
        } else {
          ownerCellCompleter.putNext(mutability)
        }
        if (mutability == Mutable || mutability == ShallowImmutable) {
          addImmutabilityReason(cellCompleterToClass.getOrElse(ownerCellCompleter, null), cellCompleterToClass.getOrElse(childCellCompleter, null))
        }
        List(mutability)
      } else {
        // Cell not complete yet
        if (classContext == ParentClassContext) {
          // If we inherit a mutable
          ownerCellCompleter.cell.whenNext(childCellCompleter.cell, (x: Immutability) => {
            if (x == Mutable) {
              addImmutabilityReason(cellCompleterToClass.getOrElse(ownerCellCompleter, null), cellCompleterToClass.getOrElse(childCellCompleter, null))
              WhenNext
            } else {
              FalsePred
            }
          }, Some(Mutable))
          // If we inherit a shallow immutable
          ownerCellCompleter.cell.whenNext(childCellCompleter.cell, (x: Immutability) => {
            if (x == ShallowImmutable) {
              addImmutabilityReason(cellCompleterToClass.getOrElse(ownerCellCompleter, null), cellCompleterToClass.getOrElse(childCellCompleter, null))
              WhenNext
            } else {
              FalsePred
            }
          }, Some(ShallowImmutable))
        } else if (classContext == ValDefinitionContext || classContext == ClassTypeParameterContext) {
          ownerCellCompleter.cell.whenNext(childCellCompleter.cell, (x: Immutability) => {
            if (x == Mutable || x == ShallowImmutable) {
              // If a val field refers to a Mutable or ShallowImmutable type
              // we set owner of the klass to be ShallowImmutable.
              addImmutabilityReason(cellCompleterToClass.getOrElse(ownerCellCompleter, null), cellCompleterToClass.getOrElse(childCellCompleter, null))
              WhenNext
            } else {
              FalsePred
            }
          }, Some(ShallowImmutable))
        }
        // Put the current value of the cell to the owner
        val mutability = childCellCompleter.cell.getResult
        ownerCellCompleter.putNext(mutability)
        List(mutability)
      }
    }

    def recursivePutMutability(typeArguments: List[Type], klassCompleter: CellCompleter[ImmutabilityKey.type, Immutability], classContext: ClassContext): List[Immutability] = {
      typeArguments match {
        case Nil => List(Immutable)
        case typeArgument :: tail => putMutability(typeArgument, klassCompleter, classContext) ::: recursivePutMutability(typeArgument.typeArgs, klassCompleter, classContext) ::: recursivePutMutability(tail, klassCompleter, classContext)
      }
    }

    def handleValAssignment(ownerKlass: Symbol, rhs: Tree, tpt: Tree, mods: Modifiers, klassCompleter: CellCompleter[ImmutabilityKey.type, Immutability]): Unit = {
      // Investigate the the assignment
      val assignedType = tpt.tpe // E.g. "Mutable"
      val assignedTypeSymbol = assignedType.typeSymbol // E.g. "class Mutable"
      val assignedTypeCompleter = classToCellCompleter.getOrElse(assignedTypeSymbol, null)
      if (assignedTypeCompleter == null) {
        // The assigned type e.g. "Mutable" did not have a cell completer
        // and could be an imported class from some unknown library.
        assignmentWithoutCellCompleter += assignedTypeSymbol
      }
      // Find mutability for e.g., "new Mutable[String, Int]()"
      // we check List("Mutable", "String", "Int").
      val investigate = assignedType :: assignedType.typeArgs
      recursivePutMutability(investigate, klassCompleter, ValDefinitionContext)
    }

    override def traverse(tree: Tree): Unit = tree match {
      case cls@ClassDef(mods, name, tparams, impl) =>
        val klass = cls.symbol
        if (!compilerGenerated(mods) && klass.isAnonymousClass) {
          // TODO:
          // Anonymous class
          // Assume immutable for now
        } else if (!compilerGenerated(mods)) {
          val klassCompleter = classToCellCompleter.getOrElse(klass, null)
          if (klassCompleter == null) {
            classesWithoutCellCompleter += klass
            Utils.log(s"Did not find cell completer for class: $klass")
            // This should never happen
            System.exit(1)
          } else {
            // TODO:
            // Line below does not make any sense?
            // klass.typeParams.map(_.tpe), recursivePutMutability(klass.typeParams.map(_.tpe), klassCompleter, ClassTypeParameterContext))

            // Check the parents of the class
            klass.tpe.parents.foreach(putMutability(_, klassCompleter, ParentClassContext))
          }
        }
        traverse(impl)

      case vd@ValDef(mods, name, tpt, rhs) =>
        val klass = vd.symbol.owner
        if (klass.isClass && !compilerGenerated(mods)) {
          // The owner of the value definition is a class
          val klassCompleter = classToCellCompleter.getOrElse(klass, null)
          if (klassCompleter == null) {
            // TODO: When does this happen?
            println(s"Did not find a cell completer for vd owner class: $klass, field: $vd", vd, mods, name, tpt, rhs)
            classesWithoutCellCompleter += klass
          } else {
            if (mods.hasFlag(MUTABLE)) {
              // It's a mutable value, e.g. "var x"
              addImmutabilityReason(cellCompleterToClass.getOrElse(klassCompleter, null), vd.symbol)
              klassCompleter.putFinal(Mutable)
            } else {
              // It's an immutable values, e.g. "val x"
              handleValAssignment(klass, rhs, tpt, mods, klassCompleter)
            }
          }
        } else if (compilerGenerated(mods) && !klass.isClass) {
          // A compiler generated method?
          // TODO comment
          // println("Compiler generated:", vd, vd.symbol, vd.tpe.typeSymbol, vd.tpe.underlying)

          // Trait thing:
          // trait Foo {
          //  var mutable: String
          //}
          // Generates:
          // abstract trait Foo extends Object {
          //   <accessor> def mutable(): String;
          //   <accessor> def mutable_=(x$1: String): Unit
          // };
          if (vd.symbol.isSetterParameter) {
            val firstParentClass = klass.ownerChain.find(_.isClass).head
            val klassCompleter = classToCellCompleter.getOrElse(firstParentClass, null)
            if (klassCompleter != null) {
              // TODO:
              // klassCompleter.putFinal(Mutable)
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

    var initialized = false

    def computeClassToCellCompleter() = {
      for ((compilationUnit, mapOfCellCompleters) <- scanComponent.compilationUnitToCellCompleters) {
        for ((klass, cellCompleter) <- mapOfCellCompleters) {
          classToCellCompleter += (klass.asInstanceOf[Symbol] -> cellCompleter) // TODO: double check if this cast works
        }
      }
    }

    def computeCellCompleterToClass() = {
      cellCompleterToClass = classToCellCompleter.map(_.swap)
    }

    override def apply(unit: CompilationUnit): Unit = {
      if (Utils.isScalaTest) {
        // If in in test, overwrite the map for each compilation unit
        classToCellCompleter = Map()
        computeClassToCellCompleter()
        computeCellCompleterToClass()
      } else if (!initialized) {
        // Do the computation once, the scan component has already
        // traversed all compilation units
        initialized = true
        computeClassToCellCompleter()
        computeCellCompleterToClass()
      }
      val mutabilityTraverser = new MutabilityTraverser()
      mutabilityTraverser.traverse(unit.body)
    }
  }

  override def newPhase(prev: Phase): StdPhase = new FirstPhase(prev)
}
