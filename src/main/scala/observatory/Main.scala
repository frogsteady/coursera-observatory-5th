package observatory

import java.time.LocalDate

import com.sksamuel.scrimage.Image

object Main extends App {


  private val temperature2000: Iterable[(LocalDate, Location, Temperature)] = Extraction.locateTemperatures(2015, "/stations.csv", "/2015.csv")
  private val yearleAverage2000: Iterable[(Location, Temperature)] = Extraction.locationYearlyAverageRecords(temperature2000)

//  val tempToColor: Array[(Temperature, Color)] = Array((-100D, Color(255,255,255)), (100D, Color(0,0,0)))

val tempToColor: Array[(Temperature, Color)] = Array(
  (60,	Color(255,255,255)),
  (32,	Color(255,0,0)),
  (12,	Color(255,	255,	0)),
  (0,	Color(0,	255,	255)),
  (-15,	Color(0,	0,	255)),
  (-27,	Color(255,	0,	255)),
  (-50,	Color(33,	0,	107)),
  (-60,	Color(0,	0,	0))
  )

//  Visualization.visualize(yearleAverage2000, tempToColor)
  Interaction.tile(yearleAverage2000, tempToColor, Tile(0,0,0)).output(new java.io.File("target/temperatures/2015/0/0-0.png"))

  Interaction.tile(yearleAverage2000, tempToColor, Tile(0,0,1)).output(new java.io.File("target/temperatures/2015/1/0-0.png"))
  Interaction.tile(yearleAverage2000, tempToColor, Tile(0,1,1)).output(new java.io.File("target/temperatures/2015/1/0-1.png"))
  Interaction.tile(yearleAverage2000, tempToColor, Tile(1,0,1)).output(new java.io.File("target/temperatures/2015/1/1-0.png"))
  Interaction.tile(yearleAverage2000, tempToColor, Tile(1,1,1)).output(new java.io.File("target/temperatures/2015/1/1-1.png"))

//  /temperatures/<year>/<zoom>/<x>-<y>.png




}
