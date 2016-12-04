package immutability

import cell._
import lattice.{Key, Lattice}

object ImmutabilityKey extends Key[Immutability] {

  def resolve[K <: Key[Immutability]](cells: Seq[Cell[K, Immutability]]): Seq[(Cell[K, Immutability], Immutability)] = {
    val conditionallyImmutableCells = cells.filter(_.getResult() == ConditionallyImmutable)
    if (conditionallyImmutableCells.nonEmpty)
      cells.map(cell => (cell, ConditionallyImmutable))
    else
      cells.map(cell => (cell, Immutable))
  }

  def fallback[K <: Key[Immutability]](cells: Seq[Cell[K, Immutability]]): Seq[(Cell[K, Immutability], Immutability)] = {
    val conditionallyImmutableCells = cells.filter(_.getResult() == ConditionallyImmutable)
    if (conditionallyImmutableCells.nonEmpty)
      conditionallyImmutableCells.map(cell => (cell, cell.getResult()))
    else
      cells.map(cell => (cell, Immutable))
  }

  override def toString = "Immutability"
}
