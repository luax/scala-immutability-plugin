package immutability

import lattice.Lattice

sealed trait Immutability

case object Mutable extends Immutability

case object ConditionallyImmutable extends Immutability

case object ShallowImmutable extends Immutability

case object PotentiallyImmutable extends Immutability

case object Immutable extends Immutability

object Immutability {

  implicit object ImmutabilityLattice extends Lattice[Immutability] {
    override def join(current: Immutability, next: Immutability): Immutability = {
      if (<=(next, current)) {
        current
      } else {
        next
      }
    }

    def <=(lhs: Immutability, rhs: Immutability): Boolean = {
      lhs == rhs || lhs == Immutable || ((lhs == ConditionallyImmutable || lhs == ShallowImmutable) && rhs != Immutable)
    }

    override def empty: Immutability = Immutable
  }

}
