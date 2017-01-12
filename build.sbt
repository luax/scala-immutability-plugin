import Dependencies._

val AssemblyJarName = "immutability_stats_plugin.jar"

lazy val commonSettings = Seq(
  version := "1.0.0",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-unchecked", "-deprecation"),
  unmanagedBase := baseDirectory.value / "../lib",
  parallelExecution in Test := false, // Turned off because of global state using "System.setProperty"
  exportJars := true
)
// Scalac command line options to install our compiler plugin.
lazy val usePluginSettings = Seq(
  scalacOptions in Compile ++= {
    val jar: File = (Keys.`package` in(plugin, Compile)).value
    // TODO:
    // Make path to plugin dynamic
    //
    val addPlugin = "-Xplugin:" + "./plugin/target/scala-2.11/plugin.jar"
    // jar.getAbsolutePath
    // add plugin timestamp to compiler options to trigger recompile of
    // main after editing the plugin. (Otherwise a 'clean' is needed.)
    val dummy = "-Jdummy=" + jar.lastModified
    Seq(addPlugin, dummy)
  }
)
// This subproject contains a Scala compiler plugin
lazy val plugin = (project in file("plugin")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies += ("org.scala-lang" % "scala-reflect" % scalaVersion.value),
    libraryDependencies += ("org.scala-lang" % "scala-library" % scalaVersion.value),
    libraryDependencies += ("org.scala-lang" % "scala-compiler" % scalaVersion.value),
    libraryDependencies ++= backendDeps,
    assemblyJarName in assembly := AssemblyJarName,
    test in assembly := {} // Don't run any tests when "assembly"
  )
lazy val demo = (project in file("demo")).
  settings(commonSettings: _*).
  settings(usePluginSettings: _*).
  settings(
    publishArtifact in Compile := false
  )
