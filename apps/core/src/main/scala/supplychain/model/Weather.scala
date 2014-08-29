package supplychain.model

import java.util.UUID

import supplychain.dataset.Namespaces

/**
 * Created by rene on 26.08.14.
 */

case class WeatherObservation(date: DateTime = DateTime.now,
                              temp: Double = 0.0,
                              prcp: Double = 0.0,
                              snow: Double = 0.0,
                              ws: WeatherStation,
                              uri: String = Namespaces.weatherObservation + UUID.randomUUID.toString) {

  def id = uri.substring(uri.lastIndexOf('/') + 1)

  override def toString(): String =
    return "uri: " + uri + ", Date: " + date.toString() + ", temp: " + temp.toString + ", prcp: " + prcp.toString + ", snow: " + snow.toString

  def getPrcpCategory(): Int = {
    if (prcp >= 25.4) return WeatherUtil.PRCP_HEAVY
    if (prcp >= 12.7) return WeatherUtil.PRCP_MID
    if (prcp >= 2.54) return WeatherUtil.PRCP_LIGHT
    return WeatherUtil.PRCP_NO
  }
}

case class WeatherStation(coords: Coordinates,
                          name: String = "",
                          uri: String = Namespaces.weatherStation + UUID.randomUUID.toString) {

  def id = uri.substring(uri.lastIndexOf('/') + 1)

  override def toString(): String =
    return "uri: " + uri + ", name: " + name + ", coordinates: " + coords.toString

}

object WeatherUtil {
  var WS_NAME_SUFIX = " nearest WeatherStation"
  val PRCP_NO    = 0
  val PRCP_LIGHT = 1
  val PRCP_MID   = 2
  val PRCP_HEAVY = 3
}

/*
case class WeatherDataDaily(
  date: DateTime = DateTime.now,
  tmin: Double = 0.0, // Minimum temperature (tenths of degrees C)
  tmax: Double = 0.0, // Maximum temperature (tenths of degrees C)
  prcp: Double = 0.0, // Precipitation (tenths of mm)
  snwd: Double = 0.0 // Snow depth (mm)
  ){
}

case class WeatherDataMonthly(
  date: DateTime = DateTime.now,
  dp01: Integer = 0, // Number of days with greater than or equal to 0.1 inch of precipitation
  dp05: Integer = 0, // Number of days with greater than or equal to 0.5 inch of precipitation
  dp10: Integer = 0, // Number of days with greater than or equal to 1.0 inch of precipitation
  tpcp: Double = 0.0, // Total precipitation
  mxsd: Double = 0.0, // Maximum snow depth
  mmnt: Double = 0.0, // Monthly Mean minimum temperature
  mmxt: Double = 0.0, // Monthly Mean maximum temperature
  mntm: Double = 0.0 // Monthly mean temperature
  ){
}
*/