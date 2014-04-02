package models

import play.api.libs.json.{Json, JsString, JsNumber, JsArray}
import scala.io.Source
import com.hp.hpl.jena.query.ResultSet
import dataset._
import play.api.libs.json.JsArray
import dataset.Supplier
import play.api.libs.json.JsString
import dataset.Connection
import play.api.libs.json.JsNumber
import dataset.Address

class SourceMapDataset(id: Int) extends Dataset {

  lazy val (suppliers, deliveries) = load()

  def query(queryStr: String): ResultSet = throw new UnsupportedOperationException()

  private def load() = {
    // Retrieve supply chain data
    val result = retrieveJson(s"http://free.sourcemap.com/services/supplychains/$id")

    val suppliers =
      for(stop <- (result \ "supplychain" \ "stops").as[JsArray].value) yield {
        // Get properties
        val id = (stop \ "id").as[JsNumber].value.toString
        val name = (stop \ "attributes" \ "title").as[JsString].value

        // Parse and convert coordinates
        val coordinatesStr = (stop \ "geometry").as[JsString].value
        val coordinates = coordinatesStr.stripPrefix("POINT(").stripSuffix(")").split(' ').map(_.toDouble)
        val (lat, lon) = meters2degrees(coordinates(0), coordinates(1))

        // Create Supplier
        Supplier(id, name, Address("", "", "", ""), Coordinates(lat, lon), Product(""))
      }

    val supplierMap = suppliers.groupBy(_.id).mapValues(_.head)

    val deliveries =
      for((hop, index) <- (result \ "supplychain" \ "hops").as[JsArray].value.zipWithIndex) yield {
        val senderId = (hop \ "from_stop_id").as[JsNumber].value.toString
        val receiverId = (hop \ "to_stop_id").as[JsNumber].value.toString

        val sender = supplierMap(senderId)
        val receiver = supplierMap(receiverId)

        Connection(index.toString, Product(""), sender, receiver)
      }

    (suppliers, deliveries)
  }

  /**
   * Retrieves Json from an URL.
   */
  private def retrieveJson(url: String) = {
    val source = Source.fromURL(url)
    val jsonStr = source.getLines().mkString("\n")
    source.close()

    Json.parse(jsonStr)
  }

  private def degrees2meters(lon: Double, lat: Double) = {
    val x = lon * 20037508.34 / 180.0
    var y = math.log(math.tan((90.0 + lat) * math.Pi / 360.0)) / (math.Pi / 180.0)
    y = y * 20037508.34 / 180.0

    (x, y)
  }

  private def meters2degrees(x: Double, y: Double) = {
    val y2 = y / 20037508.34 * 180.0

    val lat = math.atan(math.exp(y2 * (math.Pi / 180.0))) * 360.0 / math.Pi - 90.0
    val lon = x / 20037508.34 * 180.0

    (lat, lon)
  }
}
