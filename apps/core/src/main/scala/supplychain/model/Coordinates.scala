package supplychain.model

import java.lang.Math._

/**
 * Geographical coordinates.
 *
 * @param lat latitude
 * @param lon longitude
 */
case class Coordinates(lat: Double, lon: Double) {


  /**
   * Distance calculation
   *
   * @param coord the coordinates to calculate the distance to
   * @return distance between coordinate and <coord> in meters
   */
  def dist(coord: Coordinates): Double = {
    val R = 6371000; // km (change this constant to get miles)
    val dLat = (coord.lat - lat) * PI / 180
    val dLon = (coord.lon - lon) * PI / 180
    val a = sin(dLat/2) * sin(dLat/2) +
            cos(lat * PI / 180 ) * cos(coord.lat * PI / 180 ) *
            sin(dLon/2) * sin(dLon/2)
    val c = 2 * atan2(sqrt(a), sqrt(1-a))
    val d = R * c
    return d
  }

  override def toString(): String =
    return "lon: " + lon.toString + ", lat: " + lat.toString + ", which is here: http://google.com/maps/bylatlng?lat="+lat+"&lng="+lon
}
