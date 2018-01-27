package observatory

import observatory.Extraction._
import org.apache.spark.sql.{Dataset, Row}
import org.scalatest.FunSuite

trait ExtractionTest extends FunSuite {

  //  test("count") {
  //    val stations = readStations("/stations.csv")
  //    assert(stations.count()===27708,"stations count")
  //  }

  val stations = readStations("/stations.csv")
  val temperatures = readTemperatures("/1975.csv")
  val locateTemperatures = Extraction.locateTemperatures(1975, "/stations.csv", "/1975.csv")

  val debug = true

  val joined = getJoinedStationsToTemperature(stations, temperatures)
  //    test("join test") {
  //      val stations = readStations("/stations.csv")
  //      val temperatures = readTemperatures("/1975.csv")
  //      val frame = getJoinedStationsToTemperature(stations, temperatures)
  //      println(frame.collect().size)
  //    }


}