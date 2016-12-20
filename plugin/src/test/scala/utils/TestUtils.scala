package utils

import java.io.File
import java.net.URLClassLoader

import helpers.Utils
import org.scalatest.{FlatSpec, Matchers}

import scala.reflect.runtime.{universe => ru}
import scala.tools.reflect.{ToolBox, ToolBoxError};

object TestUtils extends FlatSpec with Matchers {
  val cl = getClass.getClassLoader.asInstanceOf[URLClassLoader]
  val cp = cl.getURLs.map(_.getFile).mkString(File.pathSeparator)
  val pluginPath = cp
  val mirror = ru.runtimeMirror(cl)
  val tb = mirror.mkToolBox(options = s"-Dmsg=hello -cp $cp -Xplugin:$cp") // TODO: Might have to extract plugin path instead of passing in all class paths

  def expectMutability(klass: String, immutabilityMessage: String)(code: String) {
    val mutabilityMessage = Utils.mutabilityMessage(klass, immutabilityMessage)
    Utils.setCurrentTestMessage(mutabilityMessage)
    val e = intercept[ToolBoxError] {
      // Intercept a false negative error to notify that the test was successful
      tb.eval(tb.parse(code))
    }
    e.getMessage should include(mutabilityMessage)
  }

  def expectMutability(klasses: List[String], immutabilityMessage: String)(code: String) {
    for (klass <- klasses) {
      expectMutability(klass, immutabilityMessage)(code)
    }
  }

  def expectMutability(klassesToImmutability: Map[List[String], String])(code: String): Unit = {
    for ((klasses, immutabilityMessage) <- klassesToImmutability) {
      expectMutability(klasses, immutabilityMessage)(code)
    }
  }
}

