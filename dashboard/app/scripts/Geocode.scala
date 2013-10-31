package scripts

import models.Dataset
import com.hp.hpl.jena.query.ResultSetFormatter
import java.io.{BufferedWriter, Writer, FileWriter, FileOutputStream}
import com.hp.hpl.jena.rdf.model.{Statement, ModelFactory}
import java.net.{URL, URLEncoder}
import scala.io.Source
import scala.collection.JavaConversions._
import scala.xml.XML

/**
 * A script that reads all addresses from the data set and
 * writes the coordinates the correspond to each address to a separate file.
 */
object Geocode {

  // Output file. Will be written in Turtle format.
  val outputFile = "data/coordinates.ttl"

  /**
   * Main method
   */
  def main(args: Array[String]) {
    // Create a sink for writing the coordinates
    val writer = new BufferedWriter(new FileWriter(outputFile))
    writer.write("@prefix geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> .")
    writer.newLine()

    // Retrieve all addresses and corresponding coordinates
    for{
      address <- retrieveAddresses()
      coordinates <- retrievesCoordinates(address)
    } {
      // Write coordinates
      writer.write(s"<${coordinates.uri}> geo:lat ${coordinates.lat} ; geo:lon ${coordinates.lon} .")
      writer.newLine()
      println(s"Wrote coordinates for ${address.street} in ${address.city}")
      // Wait 500ms to avoid overloading the geocooding API
      Thread.sleep(500)
    }

    // Clean up
    writer.close()
    println("Done")
  }

  /**
   * Retrieves all addresses from the data set.
   */
  private def retrieveAddresses(): Seq[Address] = {
    // Query dataset
    val resultSet = Dataset.query(
      """
        PREFIX ex: <http://geoknow.eu/wp5/ontology#>
        SELECT ?actor ?street ?postalcode ?city WHERE {
          ?actor a ex:Actor .
          ?actor ex:street ?street .
          ?actor ex:zipcode ?postalcode .
          ?actor ex:city ?city .
        }
      """).toSeq
    println(s"Found ${resultSet.size} addresses.")

    // Retrieve addresses from query results
    for(result <- resultSet) yield {
      Address(
        uri = result.getResource("actor").getURI,
        street = result.getLiteral("street").getString,
        city = result.getLiteral("city").getString,
        postalcode = result.getLiteral("postalcode").getString
      )
    }
  }

  /**
   * Retrieves the geographical coordinates for a given address.
   */
  private def retrievesCoordinates(address: Address): Option[Coordinates] = {
    // Generate query URI
    val url = s"http://nominatim.openstreetmap.org/search?format=xml" +
              s"&street=${address.street}&city=${address.city}"

    // Retrieve XML result
    val xml = XML.load(new URL(url))

    // Warn if no coordinates have been found
    if((xml \ "place").isEmpty) {
      println(s"No coordinates found for ${address.street} in ${address.city}")
    }

    // Extract coordinates from result
    for(placeNode <- (xml \ "place").headOption) yield {
      Coordinates(
        uri = address.uri,
        lat = (placeNode \ "@lat").text.toDouble,
        lon = (placeNode \ "@lon").text.toDouble
      )
    }
  }

  /**
   * Represents an address.
   *
   * @param uri The uri of the subject
   * @param street The street including the number
   * @param city The city
   * @param postalcode The postal code
   */
  private case class Address(uri: String, street: String, city: String,  postalcode: String)

  /**
   * Geographical coordinates.
   *
   * @param uri The uri of the subject
   * @param lat langitude
   * @param lon longitude
   */
  private case class Coordinates(uri: String, lat: Double, lon: Double)
}
