package immutability

import lattice.Lattice

sealed trait Immutability

case object Mutable extends Immutability

case object ConditionallyImmutable extends Immutability

case object ShallowImmutable extends Immutability

case object MutabilityUnknown extends Immutability

case object Immutable extends Immutability

object Immutability {

  implicit object ImmutabilityLattice extends Lattice[Immutability] {
    override def join(current: Immutability, next: Immutability): Immutability = {
      if (Immutability.immutabilityJoin(current, next)) {
        next
      } else {
        current
      }
    }

    override def empty: Immutability = Immutable
  }

  def immutabilityJoin(current: Immutability, next: Immutability): Boolean = {
    if (current == ShallowImmutable && next == Mutable) {
      true
    } else if (current == ConditionallyImmutable && (next == Mutable || next == ShallowImmutable)) {
      true
    } else if (current == Immutable && next != Immutable) {
      true
    } else {
      false
    }
  }

}
