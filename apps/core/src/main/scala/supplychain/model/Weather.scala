package supplychain.model

import java.util.UUID

import supplychain.dataset.Namespaces

/**
 * Created by rene on 26.08.14.
 */

case class WeatherObservation(var date: DateTime = DateTime.now,
                              var uri: String = null,
                              var tmin: Double = 0.0,
                              var tmax: Double = 0.0,
                              var prcp: Double = 0.0,
                              var snow: Double = 0.0,
                              var ws: WeatherStation = null,
                              var stationUri: String = null) {

  def id = uri.substring(uri.lastIndexOf('/') + 1)

  def temp = (tmin+tmax) / 2.0

  override def toString(): String =
    "uri: " + uri + ", Date: " + date.toString() + ", tmin: " + tmin.toString + ", tmax: " + tmax.toString + ", prcp: " + prcp.toString + ", snow: " + snow.toString

  def getPrcpCategory(): Int = {
    if (prcp >= 25.4) return WeatherUtil.PRCP_HEAVY
    if (prcp >= 12.7) return WeatherUtil.PRCP_MID
    if (prcp >= 2.54) return WeatherUtil.PRCP_LIGHT
    return WeatherUtil.PRCP_NO
  }

  def hasSnow(): Boolean = {
    return (snow > 0.0)
  }
}

case class WeatherStation(
                          coords: Coordinates,
                          name: String = "",
                          id: String = "",
                          //mindate: DateTime,
                          //maxdate: DateTime,
                          //var datamindate: DateTime = null,
                          //var datamaxdate: DateTime = null,
                          //var supplier: Supplier = null,
                          uri: String = null) {

  def uri_id = uri.substring(uri.lastIndexOf('/') + 1)

  override def toString(): String =
    return "uri: " + uri + ", id: " + id + ", name: " + name + ", coordinates: " + coords.toString

  //def getObservation(date: DateTime) = observations(date.toFormat(WeatherUtil.NCDC_DATA_FORMAT))
}

object WeatherUtil {
  var WS_NAME_SUFIX = " nearest WeatherStation"
  val PRCP_NO    = 0
  val PRCP_LIGHT = 1
  val PRCP_MID   = 2
  val PRCP_HEAVY = 3
  val NCDC_DATA_FORMAT = "yyy-MM-dd"
}