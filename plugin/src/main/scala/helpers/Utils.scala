package helpers

import java.io.File
import java.net.URLClassLoader
import scala.tools.nsc.{Global, Phase}

object Utils {
  private val scalaTestClassPathFound = getClass.getClassLoader.asInstanceOf[URLClassLoader].getURLs.map(_.getFile).mkString(File.pathSeparator).contains("scalatest")
  private val loggingEnabled = true

  def log(msg: => String): Unit = {
    if (loggingEnabled) {
      println(msg)
    }
  }

  def isScalaTest = scalaTestClassPathFound
}
