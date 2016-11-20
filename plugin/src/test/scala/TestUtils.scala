import java.io.File
import java.net.URLClassLoader

import org.scalatest.{FlatSpec, Matchers}

import scala.reflect.runtime.{universe => ru}
import scala.tools.reflect.{ToolBox, ToolBoxError}

import helpers.Utils;

object TestUtils extends FlatSpec with Matchers {
  val cl = getClass.getClassLoader.asInstanceOf[URLClassLoader]
  val cp = cl.getURLs.map(_.getFile).mkString(File.pathSeparator)
  val pluginPath = cp
  val mirror = ru.runtimeMirror(cl)
  val tb = mirror.mkToolBox(options = s"-Dmsg=hello -cp $cp -Xplugin:$cp") // TODO: Might have to extract plugin path instead of passing in all class paths

  def expectMutability(klass: String, message: String)(code: String) {
    val mutabilityMessage = Utils.mutabilityMessage(klass, message)
    System.setProperty(Utils.TestExpectedMessagePropertyStr, mutabilityMessage)
    val e = intercept[ToolBoxError] {
      // Intercept a false negative error to notify that the test was successful
      tb.eval(tb.parse(code))
    }
    e.getMessage should include(mutabilityMessage)
  }

  // TODO: Did not catch any output except errors maybe remove this code:
  //  def expect(message: String)(code: String): Unit = {
  //    val output = tb.eval(tb.parse(code)).toString
  //    output should include(message)
  //  }
}

