import Dependencies._

lazy val commonSettings = Seq(
  version := "1.0.0",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-unchecked", "-deprecation"),
  unmanagedBase := baseDirectory.value / "../lib"
)

// Scalac command line options to install our compiler plugin.
lazy val usePluginSettings = Seq(
  scalacOptions in Compile ++= {
    val jar: File = (Keys.`package` in(plugin, Compile)).value
    val addPlugin = "-Xplugin:" + jar.getAbsolutePath
    // add plugin timestamp to compiler options to trigger recompile of
    // main after editing the plugin. (Otherwise a 'clean' is needed.)
    val dummy = "-Jdummy=" + jar.lastModified
    Seq(addPlugin, dummy)
  }
)

lazy val root = (project in file(".")).
  aggregate(plugin, demo)

// This subproject contains a Scala compiler plugin
lazy val plugin = (project in file("plugin")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies += ("org.scala-lang" % "scala-reflect" % scalaVersion.value),
    libraryDependencies += ("org.scala-lang" % "scala-library" % scalaVersion.value),
    libraryDependencies += ("org.scala-lang" % "scala-compiler" % scalaVersion.value),
    libraryDependencies ++= backendDeps,
    publishArtifact in Compile := false
  )

lazy val demo = (project in file("demo")).
  settings(commonSettings: _*).
  settings(usePluginSettings: _*).
  dependsOn(plugin)
