package supplychain.dataset

import java.io.{BufferedWriter, FileWriter}
import java.net.{URL, URLEncoder}
import scala.collection.JavaConversions._
import scala.xml.XML

/**
 * A script that reads all addresses from the data set and
 * writes the coordinates the correspond to each address to a separate file.
 */
object Geocode {

  val dataset = new SchnelleckeDataset()

  // Output file. Will be written in Turtle format.
  val outputFile = "data/coordinates.ttl"

  private val countryMap = Map(
    "A" -> "Austria",
		"B" -> "Belgium",
		"CN" -> "China",
    "D" -> "Germany",
		"E" -> "Spain",
		"H" -> "Hungaria",
		"IN" -> "India",
		"KOR" -> "Korea",
		"NL" -> "Netherlands",
		"PL" ->"Poland",
		"SK" -> "Slovakia",
		"SLO" -> "Slovenia"
  )

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
      writer.write(s"<${coordinates.uri}> geo:lat ${coordinates.lat} ; geo:long ${coordinates.lon} .")
      writer.newLine()
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
    val resultSet = dataset.query(
      """
        PREFIX ex: <http://geoknow.eu/wp5/ontology#>
        SELECT ?actor ?street ?postalcode ?city ?country WHERE {
          ?actor a ex:Actor .
          ?actor ex:street ?street .
          ?actor ex:zipcode ?postalcode .
          ?actor ex:city ?city .
          ?actor ex:country ?country .
        }
      """).toSeq
    println(s"Found ${resultSet.size} addresses.")

    // Retrieve addresses from query results
    for(result <- resultSet) yield {
      Address(
        uri = result.getResource("actor").getURI,
        street = result.getLiteral("street").getString,
        city = result.getLiteral("city").getString,
        postalcode = result.getLiteral("postalcode").getString,
        country = countryMap(result.getLiteral("country").getString)
      )
    }
  }

  /**
   * Retrieves the geographical coordinates for a given address.
   */
  private def retrievesCoordinates(address: Address): Option[Coordinates] = {
    // Generate query URI
    def enc(str: String) = URLEncoder.encode(str, "utf8")

    val url = s"http://nominatim.openstreetmap.org/search?format=xml" +
              s"&street=${enc(address.street)}&city=${enc(address.city)}&country=${enc(address.country)}"

    // Retrieve XML result
    val xml = XML.load(new URL(url))

    // Extract result node from XML
    (xml \ "place").toList match {
      // At least one result found
      case placeNode :: _ => {
        // Extract coordinates from first result
        val name = (placeNode \ "@display_name").text
        val lat = (placeNode \ "@lat").text.toDouble
        val lon = (placeNode \ "@lon").text.toDouble

        println(s"Found address for ${address.uri} in ${address.street}, ${address.city}, ${address.country}:")
        println(s"$name ($lat, $lon)")
        println()

        Some(Coordinates(address.uri, lat, lon))
      }
      // No result found, i.e., no coordinates have been found
      case Nil => {
        println(s"No coordinates found for ${address.street} in ${address.city}")
        println()

        None
      }
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
  private case class Address(uri: String, street: String, city: String,  postalcode: String, country: String)

  /**
   * Geographical coordinates.
   *
   * @param uri The uri of the subject
   * @param lat langitude
   * @param lon longitude
   */
  private case class Coordinates(uri: String, lat: Double, lon: Double)
}
