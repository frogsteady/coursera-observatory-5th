package observatory

import com.sksamuel.scrimage.{Image, Pixel}
import observatory.Visualization.{interpolateColor, predictTemperature}

import scala.math._

/**
  * 3rd milestone: interactive visualization
  */
object Interaction {

  /**
    * @param tile Tile coordinates
    * @return The latitude and longitude of the top-left corner of the tile, as per http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
    */
  def tileLocation(tile: Tile): Location = {
    val (x, y, z) = (tile.x, tile.y, tile.zoom)
    Location(toDegrees(atan(sinh(Pi * (1.0 - 2.0 * y.toDouble / (1<<z))))),
    x.toDouble / (1<<z) * 360.0 - 180.0)
  }

  /**
    * @param temperatures Known temperatures
    * @param colors Color scale
    * @param tile Tile coordinates
    * @return A 256×256 image showing the contents of the given tile
    */
  def tile(temperatures: Iterable[(Location, Temperature)], colors: Iterable[(Temperature, Color)], tile: Tile): Image = {
    val width = 256
    val height = 256

    class ToPixelConverter(i: Color) { def asPixel = { Pixel(i.red, i.green, i.blue, 127) } }
    implicit def pixelsWrapper(i: Color): ToPixelConverter = new ToPixelConverter(i)




    var locations: List[Location] = List()

//    (0 until 256).foreach { column =>
//      (0 until 256).foreach{ row =>
//        val location = {
//          val (x,y,zoom) = (tile.x, tile.y, tile.zoom)
//          tileLocation(Tile(column + x*(1<<8), row + y*(1<<8), zoom + 8))
//        }
//        val color = interpolateColor(colors, predictTemperature(temperatures, location))
//        locations = locations :+ location
//      }
//    }
//    val pixels = locations.par.map(location => interpolateColor(colors, predictTemperature(temperatures, location)).asPixel).toArray




    var pixels: Array[Pixel] = Array()
    for(i <- 0 until 256) {
      for(j <- 0 until 256) {
        val location = {
          val (x,y,zoom) = (tile.x, tile.y, tile.zoom)

          tileLocation(Tile(j + x*(1<<8), i + y*(1<<8), zoom + 8))
        }
        val color = interpolateColor(colors, predictTemperature(temperatures, location))

        pixels = pixels :+ color.asPixel
      }
    }

    val myImage = Image(width, height, pixels)
    myImage
  }

  /**
    * Generates all the tiles for zoom levels 0 to 3 (included), for all the given years.
    * @param yearlyData Sequence of (year, data), where `data` is some data associated with
    *                   `year`. The type of `data` can be anything.
    * @param generateImage Function that generates an image given a year, a zoom level, the x and
    *                      y coordinates of the tile and the data to build the image from
    */
  def generateTiles[Data](
    yearlyData: Iterable[(Year, Data)],
    generateImage: (Year, Tile, Data) => Unit
  ): Unit = {

    for(yearlyDataValue <- yearlyData) {
      for (zoom <- 0 to 3) {
        for(x<-0 until 1<<zoom) {
          for(y<-0 until  1<<zoom) {
            generateImage(yearlyDataValue._1, Tile(x, y, zoom), yearlyDataValue._2)
          }
        }
      }
    }
  }

}
