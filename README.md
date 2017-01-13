# scala-stats

## Overview
A Scala compiler plugin that can be used to determine the immutability property of classes in a project.

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

One way is to use `scala-maven-plugin` plugin. Example of `pom.xml`:

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
Determines the mutability property of a class and the different mutability properties are:
* Mutable
* Shallow immutable
* Deeply immutable
* Conditionally Deeply immutable

The definition of an immutable object is an object that has a state that cannot be mutated once instantiated (created).
Immutability can be deep or shallow i.e, transitive or non-transitive.
An object is an instance of a class.

### Mutable
We determine a class to have the mutable property if an instance of that class can be mutated directly or indirectly.
A class is determined mutable if:

* The class contains any mutable field i.e., a **var** field.
* The source code of the class is unknown (unreachable from this project).

If a superclass is mutable all subclasses are also mutable.
A subclass can never have a "better" mutability than it's superclass.

### Shallow immutable
Non-transitive (shallow) immutability.
A class has the propperty shallow immutable if an instance of that class has a state that cannot be mutated but has references to other objects
that may be mutated.
A class is determined shallow immutable if:

* The class contains only immutable fields i.e., **val** definitions.
* Has a parent that is shallow immutable.
* A **val** definition has as a type that is known to be mutable or shallow immutable.
* A **val** definition has a type argument that is mutable or shallow immutable, e.g., "val list: List[Mutable] = List(Mutable)" where List is immutable.

### Deeply immutable
Transitive (deep) immutability means that all objects referred to by an immutable object must also be immutable.
A class is deep immutable if:

* All fields are **val** definitions (or lazy val).
* The type of all fields are deeply immutable.

### Conditionally Deeply immutable
A class has the conditionally deeply immutable property if:

* It is deeply immutable
* It has and depends on one or more type parameters, e.g., `immutable.List[T]`.
