package observatory

import com.sksamuel.scrimage.{Image, Pixel}

/**
  * 2nd milestone: basic visualization
  */
object Visualization {

  val power = 2
  val earth_radius = 6371D

  class ToRadiansConverter(i: Double) {
    def asRadians = {
      i * Math.PI / 180
    }

  }

  implicit def radiansWrapper(i: Double): ToRadiansConverter = new ToRadiansConverter(i)

  def calcDistance(a: Location, b: Location): Double = {

    import Math._

    val lambda = abs(b.lon-a.lon).asRadians
    val centralAngle = acos(sin(a.lat.asRadians) * sin(b.lat.asRadians) + cos(a.lat.asRadians) * cos(b.lat.asRadians) * cos(lambda))
    centralAngle * earth_radius

  }

  /**
    * @param temperatures Known temperatures: pairs containing a location and the temperature at this location
    * @param location Location where to predict the temperature
    * @return The predicted temperature at `location`
    */

  def predictTemperature(temperatures: Iterable[(Location, Temperature)], location: Location): Temperature = {

    var closestLocationOpt: Option[(Location, Temperature, Double)] = None
    def updateClosestLocation(l: Location, t: Temperature, d: Double) {
      closestLocationOpt match {
        case None => closestLocationOpt = Some((l, t, d))
        case Some(closestLocation) => if(closestLocation._3 > d) {
          closestLocationOpt = Some((l, t, d))
        }
      }
    }


    temperatures.find(f=>f._1 == location) match {
      case Some(equalLocation) => equalLocation._2
      case None =>
        val (numerator, denominator) = temperatures.foldLeft((0D, 0D)) { case (result, (loc, temp)) =>
          val distance = calcDistance(loc, location)
          if(distance<1) {
            updateClosestLocation(loc, temp, distance)
          }
          (result._1 + temp / Math.pow(distance, power), result._2 + 1 / Math.pow(distance, power))
        }

        closestLocationOpt.map(_._2)getOrElse(numerator / denominator)
    }
  }

  /**
    * @param points Pairs containing a value and its associated color
    * @param value The value to interpolate
    * @return The color that corresponds to `value`, according to the color scale defined by `points`
    */
  def interpolateColor(points: Iterable[(Temperature, Color)], value: Temperature): Color = {

    def linearInterpolation(fx0: Int, fx1: Int, x0: Double, x1: Double): Int = {
      Math.round(fx0 + (fx1 - fx0)/(x1 - x0)*(value-x0)).toInt
    }

    points.find(_._1==value) match {
      case Some(equalTemperature) => equalTemperature._2
      case None =>

        val (underTemperature, underColor) = {
          val underPoints = points.filter(_._1<value)
          if(underPoints.isEmpty) {
            return points.minBy(_._1)._2
          } else {
            underPoints.maxBy(_._1)
          }
        }


        val (aboveTemperature, aboveColor) = {
          val abovePoints = points.filter(_._1>value)
          if(abovePoints.isEmpty) {
            return points.maxBy(_._1)._2
          } else {
            abovePoints.minBy(_._1)
          }
        }

        val r = linearInterpolation(underColor.red, aboveColor.red, underTemperature, aboveTemperature)
        val g = linearInterpolation(underColor.green, aboveColor.green, underTemperature, aboveTemperature)
        val b = linearInterpolation(underColor.blue, aboveColor.blue, underTemperature, aboveTemperature)
        Color(r, g, b)
    }
  }

  /**
    * @param temperatures Known temperatures
    * @param colors Color scale
    * @return A 360×180 image where each pixel shows the predicted temperature at its location
    */
  def visualize(temperatures: Iterable[(Location, Temperature)], colors: Iterable[(Temperature, Color)]): Image = {

    val width = 360
    val height = 180

    class ToPixelConverter(i: Color) { def asPixel = { Pixel(i.red, i.green, i.blue, 255) } }
    implicit def pixelsWrapper(i: Color): ToPixelConverter = new ToPixelConverter(i)

    def createLocaltionByPixelPosition(h: Int, w: Int) = (90-h, 180+w)

    var pixels: Array[Pixel] = Array()

    for(i <- 0 until 180) {
      for(j <- 0 until 360) {
        val location = Some(createLocaltionByPixelPosition(i, j)).map(pixel=>Location(pixel._1, pixel._2)).get
        val color = interpolateColor(colors, predictTemperature(temperatures, location))

        pixels = pixels :+ color.asPixel
      }
    }

    val myImage = Image(width, height, pixels)
    myImage.output(new java.io.File("target/some-image.png"))
    myImage

  }

}

