package components

import cell.{CellCompleter, FalsePred, WhenNext, WhenNextComplete}
import helpers.Utils
import immutability._

import scala.collection.mutable.Set
import scala.tools.nsc.plugins.{PluginComponent => NscPluginComponent}
import scala.tools.nsc.{Global, Phase}

sealed trait ClassContext

case object ParentClassContext extends ClassContext

case object ValDefinitionContext extends ClassContext

class MutabilityComponent(val global: Global, val phaseName: String, val runsAfterPhase: String, val scanComponent: ScanComponent) extends NscPluginComponent {

  import global._

  import reflect.internal.Flags._

  override val runsAfter = List(runsAfterPhase)
  val PARENT_WAS_MUTABLE_ASSUMPTION = "Parent was mutable (assumption)"
  val PARENT_WAS_MUTABLE = "Parent was mutable"
  val FIELD_HAS_VAR_PUBLIC = "Field contained 'var' (public)"
  val FIELD_HAS_VAR_PRIVATE = "Field contained 'var' (private)"
  val PARENT_WAS_UNKNOWN = "Parent was unknown"
  val PARENT_WAS_SHALLOW = "Parent was shallow immutable"
  val VAL_FIELD_REFERS_TO_UNKNOWN = "Field with 'val' refers to unknown typ"
  val VAL_FIELD_REFERS_TO_MUTABLE = "Field with 'val' refers to mutable type"
  val VAL_FIELD_REFERS_TO_MUTABLE_ASSUMPTION = "Field with 'val' refers to mutable type (assumption)"
  val VAL_FIELD_REFERS_TO_SHALLOW = "Field with 'val' refers to shallow type"
  val REASONS_MAPPING = Map(
    PARENT_WAS_MUTABLE_ASSUMPTION -> "A",
    PARENT_WAS_MUTABLE -> "B",
    FIELD_HAS_VAR_PUBLIC -> "C",
    FIELD_HAS_VAR_PRIVATE -> "D",
    PARENT_WAS_UNKNOWN -> "E",
    PARENT_WAS_SHALLOW -> "F",
    VAL_FIELD_REFERS_TO_UNKNOWN -> "G",
    VAL_FIELD_REFERS_TO_MUTABLE -> "H",
    VAL_FIELD_REFERS_TO_MUTABLE_ASSUMPTION -> "I",
    VAL_FIELD_REFERS_TO_SHALLOW -> "J"
  )
  var classToCellCompleter: Map[Symbol, CellCompleter[ImmutabilityKey.type, Immutability]] = Map()
  var cellCompleterToClass: Map[CellCompleter[ImmutabilityKey.type, Immutability], Symbol] = Map()
  var classesWithoutCellCompleter: Set[Symbol] = Set()
  var assignmentWithoutCellCompleter: Set[Symbol] = Set()
  var ImmutabilityReasons = Map[Symbol, Symbol]()
  var ImmutabilityReasonsMutable = Map[Symbol, Set[String]]()
  var ImmutabilityReasonsShallow = Map[Symbol, Set[String]]()

  override def newPhase(prev: Phase): StdPhase = new FirstPhase(prev)

  class MutabilityTraverser() extends Traverser {

    def addImmutabilityReason(gotImmutability: Symbol, fromImmutability: Symbol, immutability: Immutability, reason: String): Unit = {
      if (gotImmutability == null || fromImmutability == null) {
        Utils.log("Add immutability reason was null (this should never happen")
        System.exit(0)
      }
      ImmutabilityReasons += (gotImmutability -> fromImmutability)
      Utils.log(s"The class '${gotImmutability.fullName}' went ${immutability} because '${reason}' from '${fromImmutability.fullName}'")

      if (immutability == Mutable) {
        var set = ImmutabilityReasonsMutable.getOrElse(gotImmutability, null)
        if (set == null) {
          set = Set[String]()
        }
        set += reason
        ImmutabilityReasonsMutable += gotImmutability -> set
        ImmutabilityReasonsShallow -= gotImmutability
      } else if (immutability == ShallowImmutable && !ImmutabilityReasonsMutable.contains(gotImmutability)) {
        var set = ImmutabilityReasonsShallow.getOrElse(gotImmutability, null)
        if (set == null) {
          set = Set[String]()
        }
        set += reason
        ImmutabilityReasonsShallow += gotImmutability -> set
      }
    }

    def compilerGenerated(mods: Modifiers): Boolean = {
      // Symbol is compiler-generated
      mods.hasFlag(SYNTHETIC)
    }

    def putMutability(typeArgument: Type, klassCompleter: CellCompleter[ImmutabilityKey.type, Immutability], classContext: ClassContext): List[Immutability] = {
      val typeSymbol = typeArgument.typeSymbol
      // E.g. "class Mutable"
      val typeCellCompleter = classToCellCompleter.getOrElse(typeSymbol, null)
      if (typeSymbol.isTypeParameter) {
        return List(MutabilityUnknown) // Ignore type params
      }
      if (typeCellCompleter == null) {
        // The type (e.g. Foo) did not have a cell completer and  could be an imported class from some unknown library
        // or some anonymous class/fn/
        // TODO: anonymous class/fn
        if (typeSymbol.isRefinementClass) {
          // Refinement class:
          // val foo = new A with B
          // Lookup A and B
          recursivePutMutability(typeArgument.parents, klassCompleter, ValDefinitionContext)
        } else {
          val klass = cellCompleterToClass.getOrElse(klassCompleter, null)
          // If it is a known type such as "List", "scala.collection.mutable.ArrayBuffer" etc
          var mutability = Assumptions.getImmutabilityAssumption(typeSymbol.fullName.toString)
          if (mutability == MutabilityUnknown) {
            // It was not known by looking at the type string, assume that it's mutable
            if (classContext == ParentClassContext) {
              // In the case, "class Child extends Parent { ... }" and "Parent" has "MutabilityUnknown"
              // assume "Child" to be "Mutable"
              mutability = Mutable
              addImmutabilityReason(klass, typeSymbol, Mutable, PARENT_WAS_UNKNOWN)
              klassCompleter.putFinal(mutability)
            } else if (classContext == ValDefinitionContext) {
              // In a val definition, set the class to be shallow immutable
              // since the val def points to a mutable (unknown type)
              mutability = ShallowImmutable

              // TODO: Is this join needed?
              //if (Immutability.immutabilityJoin(klassCompleter.cell.getResult, mutability)) {
              //}

              addImmutabilityReason(klass, typeSymbol, ShallowImmutable, VAL_FIELD_REFERS_TO_UNKNOWN)
              klassCompleter.putNext(mutability)
            }
          } else {
            if (mutability == Mutable) {
              if (classContext == ParentClassContext) {
                addImmutabilityReason(klass, typeSymbol, Mutable, PARENT_WAS_MUTABLE_ASSUMPTION)
              } else if (classContext == ValDefinitionContext) {
                mutability = ShallowImmutable
                addImmutabilityReason(klass, typeSymbol, Mutable, VAL_FIELD_REFERS_TO_MUTABLE_ASSUMPTION)
              }
              klassCompleter.putFinal(mutability)
            }
          }
          List(mutability)
        }
      } else {
        putMutability(typeCellCompleter, klassCompleter, classContext)
      }
    }

    def putMutability(ownerCellCompleter: CellCompleter[ImmutabilityKey.type, Immutability], childCellCompleter: CellCompleter[ImmutabilityKey.type, Immutability], classContext: ClassContext): List[Immutability] = {
      val childClass = cellCompleterToClass.getOrElse(childCellCompleter, null)
      val ownerClass = cellCompleterToClass.getOrElse(ownerCellCompleter, null)
      if (ownerCellCompleter.cell.isComplete) {
        var mutability: Immutability = null
        if (classContext == ParentClassContext) {
          // If a superclass is mutable all subclasses are also mutable.
          // A subclass can never have a "better" mutability than it's superclass.
          mutability = ownerCellCompleter.cell.getResult match {
            case Mutable => Mutable
            case ShallowImmutable => ShallowImmutable
            case _ => Immutable
          }
          if (mutability == Mutable) {
            addImmutabilityReason(childClass, ownerClass, Mutable, PARENT_WAS_MUTABLE)
          } else if (mutability == ShallowImmutable) {
            // TODO: Is join needed?
            //if (Immutability.immutabilityJoin(childCellCompleter.cell.getResult, mutability)) {
            //}
            addImmutabilityReason(childClass, ownerClass, ShallowImmutable, PARENT_WAS_SHALLOW)
          }
        } else if (classContext == ValDefinitionContext) {
          // The cell of "Foo" was complete, set owner of vd cell to the same mutability
          // If it complete it is either Mutable or ShallowImmutable.
          mutability = ownerCellCompleter.cell.getResult match {
            case Mutable => ShallowImmutable
            case ShallowImmutable => ShallowImmutable
            case _ => Immutable
          }
          if (ownerCellCompleter.cell.getResult == Mutable) {
            // TODO: Is join needed?
            //if (Immutability.immutabilityJoin(childCellCompleter.cell.getResult, mutability)) {
            //}
            addImmutabilityReason(childClass, ownerClass, ShallowImmutable, VAL_FIELD_REFERS_TO_MUTABLE)
          } else if (ownerCellCompleter.cell.getResult == ShallowImmutable) {
            // TODO: Is join needed?
            //if (Immutability.immutabilityJoin(childCellCompleter.cell.getResult, mutability)) {
            //}
            addImmutabilityReason(childClass, ownerClass, ShallowImmutable, VAL_FIELD_REFERS_TO_SHALLOW)
          }
        }
        if (mutability == Mutable) {
          childCellCompleter.putFinal(mutability)
        } else {
          childCellCompleter.putNext(mutability)
        }
        List(mutability)
      } else {
        // Cell not complete yet
        if (classContext == ParentClassContext) {
          // If we inherit a mutable
          childCellCompleter.cell.whenNext(ownerCellCompleter.cell, (x: Immutability) => {
            if (x == Mutable) {
              addImmutabilityReason(childClass, ownerClass, Mutable, PARENT_WAS_MUTABLE)
              WhenNextComplete
            } else {
              FalsePred
            }
          }, Some(Mutable))
          // If we inherit a shallow immutable
          childCellCompleter.cell.whenNext(ownerCellCompleter.cell, (x: Immutability) => {
            if (x == ShallowImmutable) {
              addImmutabilityReason(childClass, ownerClass, ShallowImmutable, PARENT_WAS_SHALLOW)
              WhenNext
            } else {
              FalsePred
            }
          }, Some(ShallowImmutable))
        } else if (classContext == ValDefinitionContext) {
          childCellCompleter.cell.whenNext(ownerCellCompleter.cell, (x: Immutability) => {
            if (x == Mutable || x == ShallowImmutable) {
              if (x == Mutable) {
                addImmutabilityReason(childClass, ownerClass, ShallowImmutable, VAL_FIELD_REFERS_TO_MUTABLE)
              } else if (x == ShallowImmutable) {
                addImmutabilityReason(childClass, ownerClass, ShallowImmutable, VAL_FIELD_REFERS_TO_SHALLOW)
              }
              // If a val field refers to a Mutable or ShallowImmutable type
              // we set owner of the klass to be ShallowImmutable.
              WhenNext
            } else {
              FalsePred
            }
          }, Some(ShallowImmutable))
        }
        val mutability = ownerCellCompleter.cell.getResult
        List(mutability)
      }
    }

    def recursivePutMutability(typeArguments: List[Type], klassCompleter: CellCompleter[ImmutabilityKey.type, Immutability], classContext: ClassContext): List[Immutability] = {
      typeArguments match {
        case Nil => List(Immutable)
        case typeArgument :: tail => putMutability(typeArgument, klassCompleter, classContext) ::: recursivePutMutability(typeArgument.typeArgs, klassCompleter, classContext) ::: recursivePutMutability(tail, klassCompleter, classContext)
      }
    }

    def handleValAssignment(tpe: Type, mods: Modifiers, klassCompleter: CellCompleter[ImmutabilityKey.type, Immutability]): Unit = {
      // Investigate the the assignment
      val assignedType = tpe
      // E.g. "Mutable"
      val assignedTypeSymbol = assignedType.typeSymbol
      // E.g. "class Mutable"
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

        if (compilerGenerated(mods)) {
          // Ignore compiler generated
          return traverse(impl)
        }

        val klassCompleter = classToCellCompleter.getOrElse(klass, null)
        if (klassCompleter == null) {
          classesWithoutCellCompleter += klass
          Utils.log(s"Did not find cell completer for class: $klass")
          // This should never happen
          System.exit(1)
        }

        if (!klassCompleter.cell.isComplete) {
          if (klass.typeParams.nonEmpty) {
            Utils.log(s"Class '${klass.fullName}' went ConditionallyImmutable. Had type params: '${klass.typeParams}'")
            klassCompleter.putNext(ConditionallyImmutable)
          }

          val publicAccessorSetters = klass.tpe.decls.collect { case f: Symbol if f.isSetter && f.isAccessor && f.isPublic => f }
          val privateAccessorSetters = klass.tpe.decls.collect { case f: Symbol if f.isSetter && f.isAccessor && !f.isPublic => f }
          for (f <- publicAccessorSetters) {
            val typeThatGetSetSymbol = f.firstParam.tpe.typeSymbol
            // It's a mutable value, e.g. "var x"
            addImmutabilityReason(klass, typeThatGetSetSymbol, Mutable, FIELD_HAS_VAR_PUBLIC)
            klassCompleter.putFinal(Mutable)
          }

          for (f <- privateAccessorSetters) {
            // It's a mutable value, e.g. "private var x"
            val typeThatGetSet = f.firstParam.tpe
            // If we "allow" private var, treat as val
            if (Utils.AllowPrivateVar) {
              handleValAssignment(typeThatGetSet, mods, klassCompleter)
            } else {
              val typeThatGetSetSymbol = typeThatGetSet.typeSymbol
              addImmutabilityReason(klass, typeThatGetSetSymbol, Mutable, FIELD_HAS_VAR_PRIVATE)
              klassCompleter.putFinal(Mutable)
            }
          }

          if (!compilerGenerated(mods)) {
            // Check the parents of the class
            klass.tpe.parents.foreach(tpe => {
              recursivePutMutability(List(tpe), klassCompleter, ParentClassContext)
            })
          } else if (!compilerGenerated(mods)) {
            // hello
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
            // Utils.log(s"Did not find a cell completer for vd owner class: $klass, field: $vd", vd, mods, name, tpt, rhs)
            classesWithoutCellCompleter += klass
          } else if (!klassCompleter.cell.isComplete) {
            if (mods.hasFlag(MUTABLE)) {
              // TODO: Var decl is
              // It's a mutable value, e.g. "var x"
              // if (!ALLOW_PRIVATE_VAR) {
              //  Utils.log(s"Class '$klass' went mutable. Definition was var.")
              //  addImmutabilityReason(cellCompleterToClass.getOrElse(klassCompleter, null), vd.symbol)
              //  klassCompleter.putFinal(Mutable)
              // }
            } else {
              // It's an immutable values, e.g. "val x" (not private)
              handleValAssignment(tpt.tpe, mods, klassCompleter)
            }
          }
        } else if (compilerGenerated(mods) && !klass.isClass) {
          // A compiler generated method?

          // TODO: Remove?
          // Trait thing:
          // trait Foo {
          //  var mutable: String
          //}
          // Generates:
          // abstract trait Foo extends Object {
          //   <accessor> def mutable(): String;pit
          //          if (vd.symbol.isSetterParameter) {
          //            val firstParentClass = klass.ownerChain.find(_.isClass).head
          //            val klassCompleter = classToCellCompleter.getOrElse(firstParentClass, null)
          //            if (klassCompleter != null) {
          //              // TODO:
          //              // klassCompleter.putFinal(Mutable)
          //            }
          //          }
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

    override def apply(unit: CompilationUnit): Unit = {
      if (Utils.isScalaTest) {
        // If in in test, overwrite the map for each compilation unit
        classToCellCompleter = Map()
        computeClassToCellCompleter()
        computeCellCompleterToClass()
        classesWithoutCellCompleter = Set[Symbol]()
        assignmentWithoutCellCompleter = Set[Symbol]()
        ImmutabilityReasons = Map[Symbol, Symbol]()
        ImmutabilityReasonsMutable = Map[Symbol, Set[String]]()
        ImmutabilityReasonsShallow = Map[Symbol, Set[String]]()
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

    def computeClassToCellCompleter(): Unit = {
      for ((compilationUnit, mapOfCellCompleters) <- scanComponent.compilationUnitToCellCompleters) {
        for ((klass, cellCompleter) <- mapOfCellCompleters) {
          classToCellCompleter += (klass.asInstanceOf[Symbol] -> cellCompleter)
        }
      }
    }

    def computeCellCompleterToClass(): Unit = {
      cellCompleterToClass = classToCellCompleter.map(_.swap)
    }
  }
}
