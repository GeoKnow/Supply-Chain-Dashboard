package supplychain.simulator

import java.io._
import java.util.GregorianCalendar
import java.util.logging.Logger

import akka.event.Logging
import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryFactory}
import supplychain.model._

import play.api.libs.json._

import scala.concurrent.{Await, Future}

import scala.collection.JavaConversions._

import scala.util.Random

/**
 * Created by rene on 27.08.14.
 */
object WeatherProvider {

  private val log = Logger.getLogger(getClass.getName)

  private var weatherStationsByLocationId: Map[String, List[WeatherStation]] = Map()
  private var weatherStationsByStationId: Map[List[String], WeatherStation] = Map()
  private var collectedDailySummaries: List[String] = List()

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
  /*def getCurrentWeather(ws: WeatherStation, d: DateTime): WeatherObservation = {
    val y = d.getYear().toString
    val m = d.getMonth()
    val r = new Random()
    //double randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();

    val variationFactor = 1 + (Random.nextDouble() * dailyWeatherVariation) - (dailyWeatherVariation / 2)

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

    new WeatherObservation(d, temp, temp, prcp, snow, ws)
  }*/

  // read weather data from local file
  //deserialize()

  /*
  def getNearesWeaterStation(supplier: Supplier, startdate: DateTime = Scheduler.simulationStartDate, enddate: DateTime = Scheduler.lastOrderDate): WeatherStation = {

    var shortestDist: Double = -1
    var nearestWs: WeatherStation = null
    var nearestWsId: String = null
    var nearestWsLocId: String = null

    if (weatherStationsByLocationId.isEmpty) getWeatherStations()

    for (locid <- weatherStationsByLocationId.keys) {
      for (ws <- weatherStationsByLocationId(locid)) {
        val dist = ws.coords.dist(supplier.coords)
        if (ws.mindate<= startdate && ws.maxdate >= enddate){
          if (shortestDist < 0 || dist < shortestDist) {
            shortestDist = dist
            nearestWs = ws
            nearestWsId = ws.id
            nearestWsLocId = locid
          }
        }
      }
    }
    nearestWs.supplier = supplier

    getDailySummarys(nearestWs, startdate, enddate)
    if (nearestWs.datamindate > startdate || nearestWs.datamaxdate < enddate) {
      log.info("searching other WS due to available data of: " + nearestWs.toString())
      nearestWs = getNearesWeaterStation(supplier, startdate, enddate)
    }

    log.info("found WS: " + nearestWs.toString())
    nearestWs
  }*/

  /*
  def getCurrentWeather(ws: WeatherStation, date: DateTime): WeatherObservation = {
    //log.info(ws.toString())
    if (!ws.observations.containsKey(date.toFormat(WeatherUtil.NCDC_DATA_FORMAT))) {
      getDailySummarys(ws, date, Scheduler.lastOrderDate)
    }
    if (!ws.observations.containsKey(date.toFormat(WeatherUtil.NCDC_DATA_FORMAT))) {
      var nextday = date + Duration.days(1)
      var previousday = date - Duration.days(1)
      while (nextday <= (Scheduler.lastOrderDate + Duration.days(30)) && previousday >= Scheduler.simulationStartDate) {
        if (ws.observations.containsKey(nextday.toFormat(WeatherUtil.NCDC_DATA_FORMAT))) {
          var wo = ws.observations(nextday.toFormat(WeatherUtil.NCDC_DATA_FORMAT))
          wo.date = date
          return wo
        } else if (ws.observations.containsKey(previousday.toFormat(WeatherUtil.NCDC_DATA_FORMAT))) {
          var wo = ws.observations(previousday.toFormat(WeatherUtil.NCDC_DATA_FORMAT))
          wo.date = date
          return wo
        }
        nextday += Duration.days(1)
        previousday -= Duration.days(1)
      }
    }
    var wo = ws.observations(date.toFormat(WeatherUtil.NCDC_DATA_FORMAT))
    wo.date = date
    return wo
  }*/

  /*
  private def getDailySummarys(ws: WeatherStation, startdate: DateTime, enddate: DateTime): WeatherStation = {
    //log.info(ws.toString())
    val queryKey = ws.id + "-" + startdate.toFormat(WeatherUtil.NCDC_DATA_FORMAT) + "-" + enddate.toFormat(WeatherUtil.NCDC_DATA_FORMAT)
    if (collectedDailySummaries.contains(queryKey)) {
      log.info("summaries cached skipping REST calls...")
      return ws
    }

    var offset = 0
    var enddate_ = enddate
    var startdate_ = startdate
    var lastGotDate = startdate
    var woMap: Map[String, WeatherObservation] = Map()
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

    var step = 0
    while (_start <= _end) {
      var _stepEnd = _end
      if ((_end - _start) > Duration.days(365)) {
        _stepEnd = _start + Duration.days(365)
      } else {
        _stepEnd = _end
      }
      val sdStr = _start.toFormat(WeatherUtil.NCDC_DATA_FORMAT)
      val edStr = _stepEnd.toFormat(WeatherUtil.NCDC_DATA_FORMAT)

      var offset = 0
      var count = 0
      var limit = 1000
      do {
        val uri = "http://www.ncdc.noaa.gov/cdo-web/api/v2/data?datasetid=GHCND&stationid=" + ws.id +
          "&startdate=" + sdStr + "&enddate=" + edStr +
          "&limit=" + limit + "&offset=" + offset
        val result = WeatherNcdc.get(uri)
        val resultResponse = Await.result(result, scala.concurrent.duration.Duration.create("600s"))
        val json = Json.parse(resultResponse)

        implicit val dailySummaryReads = Json.reads[NcdcDailySummary]
        implicit val resultsetReads = Json.reads[NcdcResultset]
        implicit val metadataReads = Json.reads[NcdcMetadata]
        implicit val dailySummaryResultsReads = Json.reads[NcdcDailySummaryResult]

        val locRes = json.as[NcdcDailySummaryResult]

        var cal = new GregorianCalendar()
        for (nds <- locRes.results) {
          val date = DateTime.parse(WeatherUtil.NCDC_DATA_FORMAT, nds.date)
          if (ws.datamindate == null || date < ws.datamindate) {
            ws.datamindate = date
          }
          if (ws.datamaxdate == null || date > ws.datamaxdate) {
            ws.datamaxdate = date
          }

          var wo: WeatherObservation = null
          if (woMap.contains(date.toFormat(WeatherUtil.NCDC_DATA_FORMAT))) {
            wo = woMap(date.toFormat(WeatherUtil.NCDC_DATA_FORMAT))
          }
          else {
            wo = new WeatherObservation(date)
            wo.ws = ws
            woMap += (date.toFormat(WeatherUtil.NCDC_DATA_FORMAT) -> wo)
          }

          if (nds.datatype == "PRCP") {
            wo.prcp = nds.value / 10
            woMap += (date.toFormat(WeatherUtil.NCDC_DATA_FORMAT) -> wo)
          }
          if (nds.datatype == "SNWD") {
            wo.snow = nds.value
            woMap += (date.toFormat(WeatherUtil.NCDC_DATA_FORMAT) -> wo)
          }
          if (nds.datatype == "TMIN") {
            wo.tmin = nds.value / 10
            woMap += (date.toFormat(WeatherUtil.NCDC_DATA_FORMAT) -> wo)
          }
          if (nds.datatype == "TMAX") {
            wo.tmax = nds.value / 10
            woMap += (date.toFormat(WeatherUtil.NCDC_DATA_FORMAT) -> wo)
          }
        }
        _start = _stepEnd + Duration.days(1)

        log.info(locRes.metadata.resultset.toString)
        count = locRes.metadata.resultset.count
        offset += limit
      } while (count > offset)
    }

    ws.observations = woMap
    collectedDailySummaries = queryKey :: collectedDailySummaries
    ws
  } */

  /*
  def serialize() = {
    val oos = new ObjectOutputStream(new FileOutputStream("WeatherStations.fos"))
    try {
      oos.writeObject(weatherStationsByLocationId)
      oos.writeObject(weatherStationsByStationId)
    } finally {
      oos.close()
    }
  }

  def deserialize() = {
    try {
      val ois = new ObjectInputStream(new FileInputStream("WeatherStations.fos"))
      try {
        weatherStationsByLocationId = ois.readObject().asInstanceOf[Map[String, List[WeatherStation]]]
        weatherStationsByStationId = ois.readObject().asInstanceOf[Map[String, WeatherStation]]
      } finally {
        ois.close()
      }
    } catch {
      case ex: IOException =>
    }
  }
  */

  /*
  private def getWeatherStations(locationid: String = "FIPS:GM"): List[WeatherStation] = {
    if (weatherStationsByLocationId.keys.contains(locationid)) {
      log.info("known location id requested, serving stations from cache...")
      return weatherStationsByLocationId(locationid)
    }

    var wss: List[WeatherStation] = List()
    var offset = 0
    var count = 0
    var limit = 1000
    do {

      // http://www.ncdc.noaa.gov/cdo-web/api/v2/stations?datasetid=GHCND&datasetid=GHCNDMS&datacategoryid=TEMP&datacategoryid=PRCP&
      val uri_stations_in_de = "http://www.ncdc.noaa.gov/cdo-web/api/v2/stations?datasetid=GHCND&datasetid=GHCNDMS&datacategoryid=TEMP&datacategoryid=PRCP&locationid=" + locationid +
        "&limit=" + limit + "&offset=" + offset
      val result = WeatherNcdc.get(uri_stations_in_de)
      val resultResponse = Await.result(result, scala.concurrent.duration.Duration.create("600s"))
      val json: JsValue = Json.parse(resultResponse)

      implicit val locationReads = Json.reads[NcdcLocation]
      implicit val resultsetReads = Json.reads[NcdcResultset]
      implicit val metadataReads = Json.reads[NcdcMetadata]
      implicit val locationResultsReads = Json.reads[NcdcLocationResults]
      val locRes = json.as[NcdcLocationResults]

      for (loc <- locRes.results) {
        val coords = new Coordinates(loc.latitude, loc.longitude)
        val mindate = DateTime.parse(WeatherUtil.NCDC_DATA_FORMAT, loc.mindate)
        val maxdate = DateTime.parse(WeatherUtil.NCDC_DATA_FORMAT, loc.maxdate)
        if (maxdate >= Scheduler.lastOrderDate) {
          val ws = new WeatherStation(coords, loc.name, loc.id, mindate, maxdate, null, null)
          wss = ws :: wss
          weatherStationsByStationId += (ws.id -> ws)
        }
      }

      if (wss.size > 0) {
        weatherStationsByLocationId += (locationid -> wss)
      }

      log.info(locRes.metadata.resultset.toString)
      count = locRes.metadata.resultset.count
      offset += limit
    } while (count > offset)

    /*
    val sd = DateTime.parse("yyyy-MM-dd", "2010-01-01")
    val ed = DateTime.parse("yyyy-MM-dd", "2013-12-31")
    val wsLE = new WeatherStation(new Coordinates(0.0, 0.0), "", "GHCND:GME00102292")
    getDailySummary(wsLE, sd, ed)
    */

    if (weatherStationsByLocationId.contains(locationid)){
      weatherStationsByLocationId(locationid)
    } else {
      List()
    }
  }
  */

  /*
  private def getAvg(map: Map[String, List[Double]], index: Int): Double = {
    var count = 0.0
    var sum = 0.0
    for ((k,v) <- map) {
      sum = sum + v(index)
      count += 1
    }
    sum/count
  }
  */

  /*
  def delayedDueToWeatherProbability(ws: WeatherStation, d: DateTime): Double = {
    log.info(ws.toString())
    val w = getCurrentWeather(ws, d)
    log.info(w.toString())
    var probab = 0.0
    if (w.getPrcpCategory() == WeatherUtil.PRCP_HEAVY) probab += 0.20
    if (w.getPrcpCategory() == WeatherUtil.PRCP_MID) probab += 0.10
    if (w.getPrcpCategory() == WeatherUtil.PRCP_LIGHT) probab += 0.5
    if (w.snow > 0.0) probab = (probab + 0.5) * 1.75
    probab += (-1 * w.temp / 100)
    if (probab < 0.0) probab = 0.0
    if (probab > 1.0) probab = 1.0
    log.info("delay probab: " + probab.toString)
    probab
  }
  */
}
