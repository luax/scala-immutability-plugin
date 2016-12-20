# scala-stats
TODO

## Overview

## Installation

## Usage

## Documentation
Determines the mutability of a class and the different mutability ratings are:
* Mutable
* Shallow immutable
* Deeply immutable

The definition of an immutable object is an object that has a state that cannot be mutated once instantiated (created).
Immutability can be deep or shallow i.e, transitive or non-transitive.
An object is an instance of a class.

### Mutable
We rate a class as mutable if an instance of that class can be mutated directly or indirectly.
A class is rated mutable if:

* The class contains any mutable field i.e., a **var** field.
* The source code of the class is unknown (unreachable from this project).

If a superclass is mutable all subclasses are also mutable.
A subclass can never have a "better" mutability than it's superclass.

### Shallow immutable
Non-transitive (shallow) immutability.
A class is rated shallow immutable if an instance of that class has a state that cannot be mutated but has references to other objects 
that may be mutated.
A class is rated shallow immutable if:

* The class contains only immutable fields i.e., **val** definitions.
* Has a parent that is shallow immutable.
* A **val** definition has as a type that is known to be mutable or shallow immutable.
* A **val** definition has a type argument that is mutable or shallow immutable, e.g., "val list: List[Mutable] = List(Mutable)" where List is immutable.

### Deeply immutable
Transitive (deep) immutability means that all objects referred to by an immutable object must also be immutable.
A class is deep immutable if:

* All fields are **val** definitions (or lazy val).
* The type of all fields are deeply immutable.

