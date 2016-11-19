import org.scalatest._

class ExampleSpec extends FlatSpec with Matchers {

  it should "fooo" in {
    TestUtils.expectError("foo") {
      """
        object Main {

          def main(args: Array[String]): Unit = {

          }
        }
      """
    }
  }

  it should "foo2o" in {
    TestUtils.expectError("asdf") {
      """
        object Main {

          def main(args: Array[String]): Unit = {

          }
        }
      """
    }
  }
}
