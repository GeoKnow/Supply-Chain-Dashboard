package supplychain.simulator

import java.text.{SimpleDateFormat, DateFormat}
import java.util.logging.Logger
import javax.swing.text.DateFormatter

import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryFactory}
import supplychain.dataset.{EndpointConfig}
import supplychain.model.{Coordinates, WeatherStation, WeatherObservation, DateTime, WeatherUtil, Duration}
import scala.collection.JavaConversions._
import scala.util.Random

/**
 * Created by rene on 09.01.15.
 */
class WeatherProvider(ec: EndpointConfig) {

  private val log = Logger.getLogger(getClass.getName)
  private val minObs = 300

  val dailyWeatherVariation = 0.1 // +/- 10% daily weather variation from monthly mean values

  private var weatherObservationByStationIdAndDate: Map[String, WeatherObservation] = Map()

  def getNearesWeaterStation(coordinates: Coordinates): WeatherStation = {

    val queryStr =
      s"""
         |PREFIX gkwo: <http://www.xybermotive.com/GeoKnowWeatherOnt#>
         |PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
         |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |
         |SELECT DISTINCT ?s ?sid ?label ?long ?lat
         |FROM <${ec.getDefaultGraphWeather()}>
         |WHERE
         |{
         |    ?s geo:long ?long .
         |    ?s geo:lat ?lat .
         |    ?s gkwo:stationId ?sid .
         |    ?s rdfs:label ?label .
         |    {
         |        SELECT DISTINCT ?s
         |        FROM <${ec.getDefaultGraphWeather()}>
         |        WHERE {
         |            ?s a gkwo:WeatherStation .
         |            ?s gkwo:hasObservation ?obs .
         |            ?obs gkwo:tmin ?tmin .
         |            ?obs gkwo:tmax ?tmax .
         |            ?obs gkwo:prcp ?prcp .
         |            ?obs gkwo:snwd ?snwd .
         |        }
         |        GROUP BY ?s
         |        HAVING (count(?obs) > ${minObs})
         |    }
         |}ORDER BY ASC (<bif:st_distance> (<bif:st_point>(?long, ?lat), <bif:st_point>(${coordinates.lon}, ${coordinates.lat}))) LIMIT 1
       """.stripMargin

    //log.info(queryStr)
    val result = ec.createEndpoint().select(queryStr).toSeq
    var ws: WeatherStation = null

    for (binding <- result) {
      val uri = binding.getResource("s").toString
      val stationId = binding.getLiteral("sid").getString
      val label = binding.getLiteral("label").getString
      val long = binding.getLiteral("long").getFloat
      val lat = binding.getLiteral("lat").getFloat
      ws = new WeatherStation(new Coordinates(long, lat), label, stationId, uri)
      ws.observations = getDailySummaries(ws)
    }
    //log.info(ws.toString())
    return ws
  }

  private def getNumberOfObservations(ws: WeatherStation): Int = {

    val queryStr =
      s"""
         |PREFIX gkwo: <http://www.xybermotive.com/GeoKnowWeatherOnt#>
         |PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
         |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |
         |SELECT (count(*) AS ?total) FROM <${ec.getDefaultGraphWeather()}>
         |WHERE {
         |  <${ws.uri}> gkwo:hasObservation ?obsuri .
         |    ?obsuri gkwo:date ?date .
         |     { ?obsuri gkwo:tmin ?tmin . }
         |     { ?obsuri gkwo:tmax ?tmax . }
         |     { ?obsuri gkwo:prcp ?prcp . }
         |     { ?obsuri gkwo:snwd ?snwd . }
         |}
       """.stripMargin

    val result = ec.createEndpoint().select(queryStr).toSeq

    var total = 0
    for (binding <- result) {
      val total = binding.getLiteral("total").getInt
    }
    //log.info(ws.toString())
    return total
  }

  def delayedDueToWeatherProbability(ws: WeatherStation, d: DateTime): Double = {
    log.info(ws.toString() + " " + d.toXSDFormat)
    val w = getDailySummary(ws, d)
    if (w == null) {
      log.info("no Daily Summary found for this Weather Station! " + ws.id + ", " + d.toFormat("yyyy-MM-dd"))
    }
    log.info(w.toString())
    var probab = 0.0
    if (w.getPrcpCategory() == WeatherUtil.PRCP_HEAVY) probab += 0.20
    if (w.getPrcpCategory() == WeatherUtil.PRCP_MID) probab += 0.10
    if (w.getPrcpCategory() == WeatherUtil.PRCP_LIGHT) probab += 0.5
    if (w.snwd > 0.0) probab = (probab + 0.5) * 1.75
    probab += (-1 * w.temp / 100)
    if (probab < 0.0) probab = 0.0
    if (probab > 1.0) probab = 1.0
    log.info("delay probab: " + probab.toString)
    probab
  }


  def getDailySummary(ws: WeatherStation, date:DateTime): WeatherObservation = {
    var wo: WeatherObservation = null
    if (!weatherObservationByStationIdAndDate.containsKey(ws.id + "-" + date)) {
      loadDailySummaries(ws)
      if (!weatherObservationByStationIdAndDate.containsKey(ws.id + "-" + date)) {
        var nextday = date + Duration.days(1)
        var previousday = date - Duration.days(1)
        while (nextday <= (Simulator().simulationEndDate + Duration.days(30)) && previousday >= Simulator().startDate) {
          if (weatherObservationByStationIdAndDate.containsKey(ws.id + "-" + nextday.toFormat(WeatherUtil.NCDC_DATA_FORMAT))) {
            var wo = weatherObservationByStationIdAndDate(ws.id + "-" + nextday.toFormat(WeatherUtil.NCDC_DATA_FORMAT))
            wo.date = date
            return wo
          } else if (weatherObservationByStationIdAndDate.containsKey(ws.id + "-" + previousday.toFormat(WeatherUtil.NCDC_DATA_FORMAT))) {
            var wo = weatherObservationByStationIdAndDate(ws.id + "-" + previousday.toFormat(WeatherUtil.NCDC_DATA_FORMAT))
            wo.date = date
            return wo
          }
          nextday += Duration.days(1)
          previousday -= Duration.days(1)
        }
      }
    } else {
      wo = weatherObservationByStationIdAndDate(ws.id + "-" + date)
    }
    return wo
  }

  def loadDailySummaries(ws: WeatherStation) = {
    val observations = getDailySummaries(ws)

    for ((k,v)<-observations) {
      weatherObservationByStationIdAndDate += (ws.id + "-" + k -> v)
    }
  }

  def getDailySummaries(ws: WeatherStation): Map[String, WeatherObservation] = {
    var observations: Map[String, WeatherObservation] = Map() // string is date in format yyyy-MM-dd

    val queryStr =
      s"""
       |PREFIX gkwo: <http://www.xybermotive.com/GeoKnowWeatherOnt#>
       |PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
       |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
       |
       |SELECT ?obsuri ?date ?tmin ?tmax ?prcp ?snwd FROM <${ec.getDefaultGraphWeather()}>
       |WHERE {
       |  <${ws.uri}> gkwo:hasObservation ?obsuri .
       |  ?obsuri gkwo:date ?date .
       |   { ?obsuri gkwo:tmin ?tmin . }
       |   { ?obsuri gkwo:tmax ?tmax . }
       |   { ?obsuri gkwo:prcp ?prcp . }
       |   { ?obsuri gkwo:snwd ?snwd . }
       |}
     """.stripMargin

    //log.info(queryStr)

    val result = ec.createEndpoint().select(queryStr).toSeq

    var wo: WeatherObservation = null

    for (binding <- result) {
      //log.info(binding.toString)
      val obsuri = binding.getResource("obsuri").toString
      val dateStr = binding.getLiteral("date").getValue.toString

      val tmin = if (binding.get("tmin") != null) binding.getLiteral("tmin").getFloat else -9999.0
      val tmax = if (binding.get("tmax") != null) binding.getLiteral("tmax").getFloat else -9999.0
      val prcp = if (binding.get("prcp") != null) binding.getLiteral("prcp").getFloat else -9999.0
      val snwd = if (binding.get("snwd") != null) binding.getLiteral("snwd").getFloat else -9999.0

      val date_ = DateTime.parse(WeatherUtil.NCDC_DATA_FORMAT, dateStr)
      //val df: DateFormat = new SimpleDateFormat("yyyy-MM-dd");
      wo = new WeatherObservation(date_, obsuri, tmin, tmax, prcp, snwd, ws, ws.uri)
      //log.info(wo.toString())
      observations += (dateStr -> wo)
    }
    return observations
  }
}

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

  // monthly mean temp in °C
  val mntm = Map(
    "2010" -> List(-5.3, -0.8,  4.8,  9.0, 10.9, 17.0, 21.8, 17.9, 13.3,  8.5, 5.0, -4.7),
    "2011" -> List( 1.3,  0.3,  5.2, 12.2, 14.6, 18.0, 17.3, 18.9, 16.3, 10.4, 4.6,  4.6),
    "2012" -> List( 1.8, -3.3,  7.7,  9.0, 15.0, 16.3, 18.8, 19.6, 15.3,  9.6, 5.5,  1.7),
    "2013" -> List(-0.2, -0.4, -1.4,  9.0, 12.9, 16.4, 20.4, 19.3, 13.9, 11.4, 4.8,  4.4)
  )

  // delay probability based on weather data LE 2010-2013
  val monthly_mean_2010_2013 = List(0.33, 0.15, 0.08, 0.01, 0.05, 0.01, 0.03, 0.02, 0.02, 0.00, 0.10, 0.18)
}
