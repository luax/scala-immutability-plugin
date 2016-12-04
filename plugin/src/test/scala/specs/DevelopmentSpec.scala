package specs

import helpers.Utils
import org.scalatest._
import utils.TestUtils

class DevelopmentSpec extends FlatSpec {

  var i = 0

  def testNr: String = {
    i += 1
    i.toString
  }

}
