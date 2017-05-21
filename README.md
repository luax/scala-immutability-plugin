## Overview
A Scala compiler plugin that analyzes a Scala project's classes at compile-time and reports the immutability property of a given class.

## Usage
To use the plugin, compile it to a `.jar` file (instructions below) and use the `-XPlugin` argument with `scalac` to compile the Scala project you want to analyze.

```
$ scalac -Xplugin:<path-to-plugin-jar>.jar Foo.scala
```

### Building the plugin

To build the plugin use `sbt` and the following commands:
```
$ sbt
$ project plugin
$ assembly
[info] Packaging <some path>/plugin/target/scala-2.11/immutability_stats_plugin.jar ...
[info] Done packaging.

```

Use the generated `.jar` file that has been generated at `<some path>/plugin/target/scala-2.11/immutability_stats_plugin.jar` when building the Scala project you want to analyze.

Note: the generated `.jar` by the `compile` command would not work as it does not include dependencies located in the `lib` folder.


### Executing the plugin on SBT projects
Add `Seq("-Xplugin:<path-to-plugin-jar>.jar"` to `scalacOptions` on a project, for example in `build.sbt` (or similar):
```
.settings(
  scalacOptions := Seq("-Xplugin:<path-to-plugin-jar>.jar"
)
```

Change the `<path-to-plugin-jar>` to a path where the plugin `.jar` is (assembled above).
For example, placing the `immutability_stats_plugin.jar` in the root of the project and the path `./immutability_stats_plugin.jar` can be used.

Now running or compiling the project would automatically use the plugin and generate statistics.

Remember to `reload` sbt in case you already had it open.

### Executing the plugin on Maven projects

One way is to use `scala-maven-plugin` plugin. An example of `pom.xml`:

```
<plugin>
  <groupId>net.alchim31.maven</groupId>
  <artifactId>scala-maven-plugin</artifactId>
  <configuration>
    <args>
      <arg>
        -Xplugin:<path-to-plugin-jar>.jar
      </arg>
    </args>
  </configuration>
</plugin>
```

The `configuration` and `-XPlugin` argument should be given a path to the plugin.

Now compiling the project should use the plugin:
```
$ mvn install
$ mvn compile
```

## Documentation
Determines the mutability property of any type of Scala class and the different mutability properties are:
* Mutable
* Shallow immutable
* Deeply immutable
* Conditionally Deeply immutable

The definition of an immutable object is an object that has a state that cannot be mutated once instantiated (created).
Immutability can be deep or shallow, i.e., transitive or non-transitive.

### Mutable
We determine a class to have the mutable property if an instance of that class can be mutated directly or indirectly.
A class is determined mutable if:

* The class contains any reassignable field, i.e., a **var** field definition.
* The source code of the class is unknown and unreachable (from this project).
* It inherits a mutable class or mixes in a mutable trait.

The property *mutable* is thus given by only inspecting the fields and the parents of a class.

### Shallow immutable
A class has the shallow (non-transitive) immutable property if the class does not have fields that can be reassigned, but has references to other objects that may be mutated (are shallow immutable or mutable).
The class is determined shallow immutable if:

* The class has only non-reassignable fields, i.e., **val** field definitions.
* Has a parent that is shallow immutable.
* A field has a type that is known to be *mutable* or *shallow immutable*.
* A field has a typeargument that is *mutable* or *shallow immutable*.

### Deeply immutable
A class is deeply (transitive) immutable if all instances of that class cannot be mutated after initialization.
A class is deep immutable if:

* All fields are non-reassignable, i.e., **val** definitions.
* The type of all fields is *deeply immutable*.

All known classes are by default assigned this property until another property holds.

### Conditionally Deeply immutable
The conditionally deeply immutable property is given to a class that is deeply immutable but depends on some other potentially mutable type. An example of this is a generic collection that can store different types, and the collection itself is declared in a way so that it cannot be mutated, but the type that is used with the collection may be *shallow immutable* or *mutable*.
A class has the *conditionally deeply immutable property* if:

* It is *deeply immutable* with one or more type parameters.
