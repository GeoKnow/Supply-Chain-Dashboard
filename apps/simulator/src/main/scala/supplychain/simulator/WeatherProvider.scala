package supplychain.simulator

import java.util.GregorianCalendar
import java.util.logging.Logger

import akka.event.Logging
import supplychain.model._

import play.api.libs.json._

import scala.concurrent.{Await, Future}

// JSON library
import play.api.libs.json.Reads._ // Custom validation helpers
import play.api.libs.functional.syntax._ // Combinator syntax

import scala.collection.JavaConversions._

import scala.util.Random

/**
 * Created by rene on 27.08.14.
 */
object WeatherProvider {

  private val log = Logger.getLogger(getClass.getName)

  private var weatherStations: Map[String, List[WeatherStation]] = Map()
  private var lastMetadata: NcdcMetadata = null

  val dailyWeatherVariation = 0.1 // +/- 10% daily weather variation from monthly mean values

  // probability for a day with rain of more that 0.1"
  val dp01 = Map( // PRCP_LIGHT
    // month         01    02    03    04    05    06    07    08   09     10    11    12
    "2010" -> List(0.13, 0.10, 0.13, 0.10, 0.43, 0.07, 0.10, 0.33, 0.20, 0.10, 0.33, 0.13),
    "2011" -> List(0.20, 0.10, 0.03, 0.10, 0.17, 0.27, 0.37, 0.20, 0.23, 0.13, 0.00, 0.13),
    "2012" -> List(0.33, 0.03, 0.00, 0.07, 0.23, 0.33, 0.30, 0.10, 0.13, 0.03, 0.13, 0.20),
    "2013" -> List(0.23, 0.13, 0.13, 0.20, 0.27, 0.20, 0.13, 0.17, 0.07, 0.13, 0.23, 0.13)
  )

  // probability for a day with rain of more that 0.5"
  val dp05 = Map( // PRCP_MID
    "2010" -> List(0.00, 0.00, 0.00, 0.00, 0.10, 0.00, 0.07, 0.13, 0.17, 0.00, 0.07, 0.00),
    "2011" -> List(0.00, 0.00, 0.00, 0.00, 0.00, 0.03, 0.17, 0.03, 0.03, 0.00, 0.00, 0.00),
    "2012" -> List(0.00, 0.00, 0.00, 0.00, 0.07, 0.07, 0.00, 0.00, 0.00, 0.00, 0.07, 0.03),
    "2013" -> List(0.00, 0.00, 0.00, 0.00, 0.10, 0.10, 0.00, 0.07, 0.03, 0.03, 0.00, 0.00)
  )

  // probability for a day with rain of more that 1"
  val dp10 = Map( // PRCP_HEAVY
    "2010" -> List(0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.07, 0.07, 0.00, 0.00, 0.00),
    "2011" -> List(0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.07, 0.03, 0.03, 0.00, 0.00, 0.00),
    "2012" -> List(0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00),
    "2013" -> List(0.00, 0.00, 0.00, 0.00, 0.00, 0.03, 0.00, 0.00, 0.03, 0.00, 0.00, 0.00)
  )

  /*
  // Maximum snow depth
  val mxsd = Map(
    "2010" -> List(340.0, 120.0,  70.0,  0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 140.0, 340.0),
    "2011" -> List(270.0,  20.0,   0.0,  0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,   0.0,   0.0),
    "2012" -> List(100.0, 110.0,   0.0,  0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,   0.0, 130.0),
    "2013" -> List(120.0, 140.0, 140.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,   0.0,   0.0)
  ) */

  // monthly mean temp in °C
  val mntm = Map(
    "2010" -> List(-5.3, -0.8,  4.8,  9.0, 10.9, 17.0, 21.8, 17.9, 13.3,  8.5, 5.0, -4.7),
    "2011" -> List( 1.3,  0.3,  5.2, 12.2, 14.6, 18.0, 17.3, 18.9, 16.3, 10.4, 4.6,  4.6),
    "2012" -> List( 1.8, -3.3,  7.7,  9.0, 15.0, 16.3, 18.8, 19.6, 15.3,  9.6, 5.5,  1.7),
    "2013" -> List(-0.2, -0.4, -1.4,  9.0, 12.9, 16.4, 20.4, 19.3, 13.9, 11.4, 4.8,  4.4)
  )

  /*
  val monthly_weather_influence = Map(
    "2010" -> List(0.46, 0.18, 0.09, 0.00, 0.16, 0.00, 0.00, 0.09, 0.08, 0.00, 0.29, 0.45),
    "2011" -> List(0.36, 0.07, 0.00, 0.00, 0.00, 0.00, 0.13, 0.00, 0.00, 0.00, 0.00, 0.02),
    "2012" -> List(0.25, 0.16, 0.00, 0.00, 0.00, 0.04, 0.00, 0.00, 0.00, 0.00, 0.05, 0.23),
    "2013" -> List(0.24, 0.21, 0.22, 0.02, 0.05, 0.00, 0.00, 0.00, 0.00, 0.00, 0.07, 0.02)
  )
  */

  // delay probability based on weather data LE 2010-2013
  val monthly_mean_2010_2013 = List(0.33, 0.15, 0.08, 0.01, 0.05, 0.01, 0.03, 0.02, 0.02, 0.00, 0.10, 0.18)

  /**
   * Generating some weather based on weather statistics
   */
  def getCurrentWeather(ws: WeatherStation, d: DateTime): WeatherObservation = {
    val y = d.getYear().toString
    val m = d.getMonth()
    val r = new Random()
    //double randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();

    var variationFactor = 1 + (Random.nextDouble() * dailyWeatherVariation) - (dailyWeatherVariation / 2)

    var temp = 0.0
    var prcp = 0.0
    var snow = 0.0
    val prcpProp = r.nextDouble()

    if (2010 <= d.getYear() && d.getYear() <= 2013) {
      if (prcpProp <= dp10(y)(m) * variationFactor) prcp = 25.4 + (500.0 - 25.4) * r.nextDouble()
      else if (prcpProp <= dp05(y)(m) * variationFactor) prcp = 12.7 + (25.4 - 12.7) * r.nextDouble()
      else if (prcpProp <= dp01(y)(m) * variationFactor) prcp = 2.54 + (12.7 - 2.54) * r.nextDouble()

      temp = mntm(y)(m) * variationFactor
    } else {
      if (prcpProp <= getAvg(dp10, m) * variationFactor) prcp = 25.4 + (500.0 - 25.4) * r.nextDouble()
      else if (prcpProp <= getAvg(dp05, m) * variationFactor) prcp = 12.7 + (25.4 - 12.7) * r.nextDouble()
      else if (prcpProp <= getAvg(dp01, m) * variationFactor) prcp = 2.54 + (12.7 - 2.54) * r.nextDouble()

      temp = getAvg(mntm, m) * variationFactor
    }

    if (temp < 0.0) {
      snow = prcp
      prcp = prcp / ( 1 + r.nextInt(9) )
    }

    val w = new WeatherObservation(d, temp, prcp, snow, ws)
    return w
  }

  def getNearesWeaterStation(coords: Coordinates): WeatherStation = {
    var shortestDist: Double = -1
    var nearestWs: WeatherStation = null

    if (weatherStations.isEmpty) getWeatherStations()

    for (locid <- weatherStations.keys) {
      for (ws <- weatherStations(locid)) {
        val dist = ws.coords.dist(coords)
        if (shortestDist < 0 || dist < shortestDist) {
          shortestDist = dist
          nearestWs = ws
        }
      }
    }
    return nearestWs
  }

  def getDailySummary(ws: WeatherStation, startdate: DateTime, enddate: DateTime): List[WeatherObservation] = {
    var offset = 0
    var enddate_ = enddate
    var startdate_ = startdate
    var woMap: Map[DateTime, WeatherObservation] = Map()
    val now = DateTime.now

    var _start = now
    var _end = now

    if (enddate_ > now) {
      enddate_ = now
    }

    if (startdate_ <= enddate_) {
      _start = startdate_
      _end = enddate_
    } else {
      _start = enddate_
      _end = startdate_
    }

    val step = _start + Duration.days(365)
    while (_start <= _end) {
      var _stepEnd = now
      if ((_end - _start) > Duration.days(365)) {
        _stepEnd = _start + Duration.days(365)
      } else {
        _stepEnd = _end
      }
      val edStr = _start.toFormat("yyyy-MM-dd")
      val sdStr = _stepEnd.toFormat("yyyy-MM-dd")

      val uri = "http://www.ncdc.noaa.gov/cdo-web/api/v2/data?datasetid=GHCND&stationid=" + ws.id + "&startdate=" + sdStr + "&enddate=" + edStr + "&limit=1000&offset=" + offset
      val result = new WeatherNCDCProvider().get(uri)
      val res: NcdcDailySummaryResult = new NcdcGson().getDailySummaryResult(result)

      var cal = new GregorianCalendar()
      for (nds <- res.getResults){
        val date = DateTime.parse("yyyy-MM-dd", nds.getDate)

        var wo: WeatherObservation = null
        if (woMap.contains(date)) {
          wo = woMap(date)
        }
        else {
          wo = new WeatherObservation(date)
          wo.ws = ws
        }

        if (nds.getDatatype == "PRCP") {
          wo.prcp = nds.getValue / 10
        }

        if (nds.getDatatype == "SNWD") {
          wo.snow = nds.getValue
        }

        if (nds.getDatatype == "TMIN") {
          wo.snow = nds.getValue
        }


      }

      /*
      cal.set(2010, 0, 1, 0, 0, 0);
      val date =

      val wo = new WeatherObservation(date =
      temp: Double = 0.0,
      prcp: Double = 0.0,
      snow: Double = 0.0,
      ws: WeatherStation,)

      wol = wo :: wol
  */
      _start = _stepEnd + Duration.days(1)
    }



    //System.out.println(res.getMetadata.getResultset.getCount)
    return null
  }

  def getWeatherStations(locationid: String = "FIPS:GM"): List[WeatherStation] = {
    if (weatherStations.keys.contains(locationid)) {
      log.info("known location id requested, serving stations from cache...")
      return weatherStations(locationid)
    }

    var wss: List[WeatherStation] = List()
    val uri_stations_in_de = "http://www.ncdc.noaa.gov/cdo-web/api/v2/stations?locationid=" + locationid + "&limit=1000"

    //val result = new WeatherNCDCProvider().get(uri_stations_in_de)

    val result = WeatherNcdc.get(uri_stations_in_de)

    val json = Await.result(result, scala.concurrent.duration.Duration.create("10s"))

    //val result = WeatherNcdc.get(uri_stations_in_de)

    //val json: JsValue = Json.parse(result)

    implicit val locationReads = Json.reads[NcdcLocation]
    implicit val resultsetReads = Json.reads[NcdcResultset]
    implicit val metadataReads = Json.reads[NcdcMetadata]
    implicit val locationResultsReads = Json.reads[NcdcLocationResults]

    System.out.println(json.as[NcdcLocationResults].toString)

    /*
    for (result <- json \\ "results") {
      val wert = (result \ "id").as[String]
      result.as

    }
    */
    //val res: NcdcLocationResult = new NcdcGson().getLocationResult(result)

    /*
    for (loc <- res.getResults) {
      val coords = new Coordinates(loc.getLatitude, loc.getLongitude)
      val ws = new WeatherStation(coords, loc.getName)
      System.out.println(ws.toString())
      wss = ws :: wss
    }

    if (wss.size > 0) {
      weatherStations += (locationid -> wss)
    }

    System.out.println(res.getMetadata.getResultset.getCount.toString)



    val sdCal = new GregorianCalendar()
    sdCal.set(2010, 0, 1, 0, 0, 0)
    val sd = new DateTime(sdCal.getTimeInMillis)

    val edCal = new GregorianCalendar()
    edCal.set(2013, 11, 31, 0, 0, 0)
    val ed = new DateTime(edCal.getTimeInMillis)

    val wsLE = new WeatherStation(new Coordinates(0.0, 0.0), "", "GHCND:GME00102292")
    */

    //getDailySummary(wsLE, sd, ed)

    return weatherStations(locationid)
  }

  private def getAvg(map: Map[String, List[Double]], index: Int): Double = {
    var count = 0.0
    var sum = 0.0
    for ((k,v) <- map) {
      sum = sum + v(index)
      count += 1
    }
    sum/count
  }

  def delayedDueToWeatherProbability(ws: WeatherStation, d: DateTime): Double = {
    val w = getCurrentWeather(ws, d)
    var probab = 0.0
    if (w.getPrcpCategory() == WeatherUtil.PRCP_HEAVY) probab += 0.20
    if (w.getPrcpCategory() == WeatherUtil.PRCP_MID) probab += 0.10
    if (w.getPrcpCategory() == WeatherUtil.PRCP_LIGHT) probab += 0.5
    if (w.snow > 0.0) probab = (probab + 0.5) * 1.75
    probab += (-1 * w.temp / 100)
    if (probab < 0.0) probab = 0.0
    if (probab > 1.0) probab = 1.0
    probab

    /*
    val y = d.getYear().toString
    val m = d.getMonth()
    var probab = 0.0

    if (2010 <= d.getYear() && d.getYear() <= 2013) {
      // temp < 0 °C assuming snow
      if (mntm(y)(m) < 0.0) probab = ( ( dp01(y)(m) + dp05(y)(m) + dp10(y)(m) ) * 1.75 ) + ( -1 * mntm(y)(m) / 100 )
      // no snow
      else probab = ( ( dp01(y)(m) + dp05(y)(m) + dp10(y)(m) ) / 2 ) + ( -1 * mntm(y)(m) / 100 )
    } else {
      // temp < 0 °C assuming snow
      if (getAvg(mntm, m) < 0.0) probab = ( ( getAvg(dp01, m) + getAvg(dp05, m) + getAvg(dp10, m) ) * 1.75 ) + ( -1 * getAvg(mntm, m) / 100 )
      // no snow
      else probab = ( ( getAvg(dp01, m) + getAvg(dp05, m) + getAvg(dp10, m) ) / 2 ) + ( -1 * getAvg(mntm, m) / 100 )
    }
    if (probab < 0.0) probab = 0.0
    if (probab > 1.0) probab = 1.0
    probab
    */
  }
}
