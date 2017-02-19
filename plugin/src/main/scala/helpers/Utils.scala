package helpers

import java.io.File
import java.net.URLClassLoader

import cell.HandlerPool

object Utils {
  val TestExpectedMessagePropertyStr = "plugin.test.expected.message"
  val IsMutable = "mutable"
  val IsShallowImmutable = "shallow immutable"
  val IsDeeplyImmutable = "deep immutable"
  val IsConditionallyImmutable = "conditionally immutable"
  // Treat private var as "val"
  val AllowPrivateVar = false
  // Assume that certain types are immutable/mutable e.g., "scala.collection.immutable.list" is immutable.
  val MakeAssumptionAboutTypes = false
  private val TheScalaTestProject = false
  // TODO: Temporary fix, for when analyzing Scala test project itself.
  private val ScalaTestPattern = "scalatest"
  private val ScalaTestClassPathFound = !TheScalaTestProject && getClass.getClassLoader.asInstanceOf[URLClassLoader].getURLs.map(_.getFile).mkString(File.pathSeparator).contains(ScalaTestPattern)

  private val LoggingEnabled = !isScalaTest

  private var pool: HandlerPool = _

  def log(msg: => String): Unit = {
    if (LoggingEnabled) {
      println(s"[log] ${msg}")
    }
  }

  def mutabilityMessage(className: String, message: String): String = s"class $className is $message"

  def getCurrentTestMessage: String = System.getProperty(TestExpectedMessagePropertyStr)

  def setCurrentTestMessage(mutabilityMessage: String): Unit = System.setProperty(Utils.TestExpectedMessagePropertyStr, mutabilityMessage)

  def isScalaTest = ScalaTestClassPathFound

  def getPool: HandlerPool = {
    if (pool == null) {
      newPool
    }
    pool
  }

  def newPool = {
    pool = new HandlerPool()
  }
}
