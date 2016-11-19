import java.io.File
import java.net.URLClassLoader

import org.scalatest.{FlatSpec, Matchers}

import scala.reflect.runtime.{universe => ru}
import scala.tools.reflect.{ToolBox, ToolBoxError}

object TestUtils extends FlatSpec with Matchers {

  val cl = getClass.getClassLoader.asInstanceOf[URLClassLoader]
  val cp = cl.getURLs.map(_.getFile).mkString(File.pathSeparator)
  val pluginPath = cp
  val mirror = ru.runtimeMirror(cl)
  val tb = mirror.mkToolBox(options = s"-cp $cp -Xplugin:$cp") // TODO: Might have to extract plugin path

  def expectError(errorSnippet: String)(code: String) {
    val e = intercept[ToolBoxError] {
      tb.eval(tb.parse(code))
    }
    e.getMessage should include(errorSnippet)
  }

  // TODO: Did not catch any output except errors maybe remove this code:
  //  def expect(message: String)(code: String): Unit = {
  //    val output = tb.eval(tb.parse(code)).toString
  //    output should include(message)
  //  }
}

