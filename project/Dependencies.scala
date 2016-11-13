import sbt._

object Dependencies {
  // Versions
  lazy val scalatestVersion = "3.0.0"

  // Libraries
  val scalatic = "org.scalactic" %% "scalactic" % scalatestVersion
  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion

  // Projects
  val backendDeps = Seq(scalatic, scalatest % Test)
}
