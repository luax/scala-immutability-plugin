package helpers

import java.io.File
import java.net.URLClassLoader

import cell.HandlerPool

object Utils {
  private val ScalaTestPattern = "scalatest"
  private val LoggingEnabled = true
  private val scalaTestClassPathFound = getClass.getClassLoader.asInstanceOf[URLClassLoader].getURLs.map(_.getFile).mkString(File.pathSeparator).contains(ScalaTestPattern)

  val TestExpectedMessagePropertyStr = "plugin.test.expected.message"
  val IsMutable = "mutable"
  val IsShallowImmutable = "shallow immutable"
  val IsDeeplyImmutable = "deep immutable"
  val IsConditionallyImmutable = "conditionally immutable"

  // Treat private var as "val"
  val AllowPrivateVar = false
  if (AllowPrivateVar) {
    println(s"NOTE: Private var is treated as val (not assigned mutable)")
  }

  // Assume that certain types are immutable/mutable e.g., "scala.collection.immutable.list" is immutable.
  val MakeAssumptionAboutTypes = true
  if (MakeAssumptionAboutTypes) {
    println(s"NOTE: Assuming that certain types are immutable/mutable e.g., 'scala.collection.immutable.list' is immutable")
  }

  private var pool: HandlerPool = _

  def log(msg: => String): Unit = {
    if (LoggingEnabled) {
      println(s"[log] ${msg}")
    }
  }

  def mutabilityMessage(className: String, message: String): String = s"class $className is $message"

  def getCurrentTestMessage: String = System.getProperty(TestExpectedMessagePropertyStr)

  def setCurrentTestMessage(mutabilityMessage: String): Unit = System.setProperty(Utils.TestExpectedMessagePropertyStr, mutabilityMessage)

  def isScalaTest = scalaTestClassPathFound

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
