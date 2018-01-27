package observatory

import java.nio.file.Paths
import java.time.LocalDate

import org.apache.spark.sql.functions.{concat_ws, lit, when}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Dataset, SparkSession}
/**
  * 1st milestone: data extraction
  */
object Extraction {

  val spark: SparkSession =
    SparkSession
      .builder()
      .appName("Time Usage")
      .config("spark.master", "local")
      .getOrCreate()

  // For implicit conversions like converting RDDs to DataFrames
  import spark.implicits._

  case class Station(identifier: String, latitude: Double, longitude: Double)
  case class Temperatures(identifier: String, month: Int, day: Int, temperature: Double)
  case class Joins(identifier: String, month: Int, day: Int, latitude: Double, longitude: Double, temperature: Double)

  def fsPath(resource: String): String =
    Paths.get(getClass.getResource(resource).toURI).toString

  def readStations = (stationsFile: String) => {

    val frame = spark.read.csv(fsPath(stationsFile))

    frame.select(concat_ws("-", when('_c0.isNotNull, '_c0).otherwise(lit("")), when('_c1.isNotNull, '_c1).otherwise(lit(""))).as("Identifier").cast(StringType),
      '_c2.as("Latitude").cast(DoubleType),
      '_c3.as("Longitude").cast(DoubleType))
      .where('Latitude.isNotNull && 'Longitude.isNotNull && 'Latitude =!= 0 && 'Longitude =!= 0 && 'Identifier.isNotNull)
      .map(row=>Station(row.getString(0), row.getDouble(1), row.getDouble(2)))

  }

  def readTemperatures = (temperatureFile: String) => {

    val frame = spark.read.csv(fsPath(temperatureFile))

    frame.select(concat_ws("-", when('_c0.isNotNull, '_c0).otherwise(lit("")), when('_c1.isNotNull, '_c1).otherwise(lit(""))).as("Identifier").cast(StringType),
      '_c2.as("Month").cast(IntegerType),
      '_c3.as("Day").cast(IntegerType),
      (('_c4 - 32) * (5.0 / 9.0)).as("Temperature").cast(DoubleType))
      .filter('Identifier.isNotNull)
      .map(row=>Temperatures(row.getString(0), row.getInt(1), row.getInt(2), row.getDouble(3)))

  }

  def getJoinedStationsToTemperature(stations: Dataset[Station], temperatures: Dataset[Temperatures]) = {

    val stationsDF = stations.toDF()
    val temperaturesDF = temperatures.toDF()

    stationsDF.join(temperatures, temperaturesDF.col("identifier") === stationsDF.col("identifier")).drop(stationsDF("identifier"))
      .select("identifier", "month", "day", "latitude", "longitude", "temperature")
      .map(a => Joins(a.getString(0), a.getInt(1), a.getInt(2), a.getDouble(3), a.getDouble(4), a.getDouble(5)))

  }

  /**
    * @param year             Year number
    * @param stationsFile     Path of the stations resource file to use (e.g. "/stations.csv")
    * @param temperaturesFile Path of the temperatures resource file to use (e.g. "/1975.csv")
    * @return A sequence containing triplets (date, location, temperature)
    */
  def locateTemperatures(year: Year, stationsFile: String, temperaturesFile: String): Iterable[(LocalDate, Location, Temperature)] = {
    val stations = readStations(stationsFile)
    val temperatures = readTemperatures(temperaturesFile)


    val frame = getJoinedStationsToTemperature(stations, temperatures)
      .select("Month", "Day", "Latitude", "Longitude", "Temperature")
      .map(a => (year, a.getInt(0), a.getInt(1), Location(a.getDouble(2), a.getDouble(3)), a.getDouble(4)))

    frame.collect().par.map(v=>(LocalDate.of(v._1, v._2, v._3), v._4, v._5)).seq

  }

  /**
    * @param records A sequence containing triplets (date, location, temperature)
    * @return A sequence containing, for each location, the average temperature over the year.
    */
  def locationYearlyAverageRecords(records: Iterable[(LocalDate, Location, Temperature)]): Iterable[(Location, Temperature)] = {

    records.par.groupBy(_._2).foldLeft(Seq[(Location, Temperature)]()) {(a,b)=>
      val count = b._2.size
      val allTemperatures = b._2.foldLeft(0D) {
        (x, y) => x + y._3
      }
      a.+:((b._1, allTemperatures/count))
    }

  }

}
