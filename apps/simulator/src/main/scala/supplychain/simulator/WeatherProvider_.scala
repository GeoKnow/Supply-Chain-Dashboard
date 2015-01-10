package supplychain.simulator

import java.text.{SimpleDateFormat, DateFormat}
import java.util.logging.Logger
import javax.swing.text.DateFormatter

import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryFactory}
import supplychain.dataset.RdfWeatherDataset
import supplychain.model.{Coordinates, WeatherStation, WeatherObservation, DateTime, WeatherUtil, Duration}
import scala.collection.JavaConversions._
import scala.util.Random

/**
 * Created by rene on 09.01.15.
 */
class WeatherProvider_(dataset: RdfWeatherDataset) {

  private val log = Logger.getLogger(getClass.getName)
  private val minObs = 1200

  val dailyWeatherVariation = 0.1 // +/- 10% daily weather variation from monthly mean values

  private var weatherObservationByStationIdAndDate: Map[String, WeatherObservation] = Map()

  def getNearesWeaterStation(coordinates: Coordinates): WeatherStation = {

    val queryStr =
      s"""
        |PREFIX gkwo: <http://www.xybermotive.com/GeoKnowWeatherOnt#>
        |PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
        |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        |
        |SELECT ?s ?sid ?label ?long ?lat FROM <http://www.xybermotive.com/GeoKnowWeather#>
        |WHERE {
        |    ?s a gkwo:WeatherStation ;
        |        geo:long ?long ;
        |        geo:lat ?lat ;
        |        gkwo:stationId ?sid ;
        |        rdfs:label ?label .
        |    ?s gkwo:hasObservation ?obs .
        |}
        |GROUP BY ?s ?sid ?label ?long ?lat
        |HAVING (count(?obs) > ${minObs})
        |ORDER BY ASC (<bif:st_distance> (<bif:st_point>(?long, ?lat), <bif:st_point>(${coordinates.lon}, ${coordinates.lat}))) LIMIT 1
      """.stripMargin

    //log.info(queryStr)
    val result = dataset.select(queryStr).toSeq
    var ws: WeatherStation = null

    for (binding <- result) {
      val uri = binding.getResource("s").toString
      val stationId = binding.getLiteral("sid").getString
      val label = binding.getLiteral("label").getString
      val long = binding.getLiteral("long").getFloat
      val lat = binding.getLiteral("lat").getFloat
      ws = new WeatherStation(new Coordinates(long, lat), label, stationId, uri)
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
         |SELECT (count(*) AS ?total) FROM <http://www.xybermotive.com/GeoKnowWeather#>
         |WHERE {
         |  <http://www.xybermotive.com/GeoKnowWeather#GME00111430> gkwo:hasObservation ?obsuri .
         |    ?obsuri gkwo:date ?date .
         |    OPTIONAL { ?obsuri gkwo:tmin ?tmin . }
         |    OPTIONAL { ?obsuri gkwo:tmax ?tmax . }
         |    OPTIONAL { ?obsuri gkwo:prcp ?prcp . }
         |    OPTIONAL { ?obsuri gkwo:snwd ?snwd . }
         |}
       """.stripMargin

    val result = dataset.select(queryStr).toSeq

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
      log.info("no Daily Summary found for this Weather Station!")
    }
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


  def getDailySummary(ws: WeatherStation, date:DateTime): WeatherObservation = {
    var wo: WeatherObservation = null
    if (!weatherObservationByStationIdAndDate.containsKey(ws.id + "-" + date)) {
      loadDailySummaries(ws)
      if (!weatherObservationByStationIdAndDate.containsKey(ws.id + "-" + date)) {
        var nextday = date + Duration.days(1)
        var previousday = date - Duration.days(1)
        while (nextday <= (Scheduler.lastOrderDate + Duration.days(30)) && previousday >= Scheduler.simulationStartDate) {
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
    val queryStr =
      s"""
         |PREFIX gkwo: <http://www.xybermotive.com/GeoKnowWeatherOnt#>
         |PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
         |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |
         |SELECT ?obsuri ?date ?tmin ?tmax ?prcp ?snwd FROM <http://www.xybermotive.com/GeoKnowWeather#>
         |WHERE {
         |  <${ws.uri}> gkwo:hasObservation ?obsuri .
         |  ?obsuri gkwo:date ?date .
         |  OPTIONAL { ?obsuri gkwo:tmin ?tmin . }
         |  OPTIONAL { ?obsuri gkwo:tmax ?tmax . }
         |  OPTIONAL { ?obsuri gkwo:prcp ?prcp . }
         |  OPTIONAL { ?obsuri gkwo:snwd ?snwd . }
         |}
       """.stripMargin

    //log.info(queryStr)

    val result = dataset.select(queryStr).toSeq

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
      weatherObservationByStationIdAndDate += (ws.id + "-" + dateStr -> wo)
    }
  }
}
