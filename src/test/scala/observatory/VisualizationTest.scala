package observatory


import org.scalatest.FunSuite
import org.scalatest.prop.Checkers

trait VisualizationTest extends FunSuite with Checkers {

  val kievLocation = Location(50.4547, 30.5238)
  val moscowLocation = Location(55.7522, 37.6156)

  test("distance between kiev and moscow") {

    assert(755 == Math.round(Visualization.calcDistance(kievLocation, moscowLocation)))

  }

}
