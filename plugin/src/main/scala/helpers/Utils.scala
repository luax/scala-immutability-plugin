package helpers

import java.io.File
import java.net.URLClassLoader

import scala.tools.nsc.{Global, Phase}

object Utils {
  private val ScalaTestPattern = "scalatest"
  private val LoggingEnabled = true
  private val scalaTestClassPathFound = getClass.getClassLoader.asInstanceOf[URLClassLoader].getURLs.map(_.getFile).mkString(File.pathSeparator).contains(ScalaTestPattern)

  val TestExpectedMessagePropertyStr = "plugin.test.expected.message"

  val IsMutable = "mutable"
  val IsShallowImmutable = "shallow immutable"
  val IsDeeplyImmutable = "deep immutable"

  def log(msg: => String): Unit = {
    if (LoggingEnabled) {
      println(msg)
    }
  }

  def mutabilityMessage(className: String, message: String): String = s"class $className is $message"

  def getCurrentTestMessage: String = System.getProperty(TestExpectedMessagePropertyStr)

  def isScalaTest = scalaTestClassPathFound


  // TODO constants
}
