package scripts

import models.Dataset
import com.hp.hpl.jena.query.ResultSetFormatter
import java.io.{FileWriter, FileOutputStream}
import com.hp.hpl.jena.rdf.model.{Statement, ModelFactory}
import java.net.{URL, URLEncoder}
import scala.io.Source
import scala.collection.JavaConversions._
import scala.xml.XML

object Geocode extends App {

  val resultSet = Dataset.query(
    """
      PREFIX ex: <http://geoknow.eu/wp5/ontology#>
      SELECT ?actor ?street ?postalcode ?city ?country WHERE {
        ?actor a ex:Actor .
        ?actor ex:street ?street .
        ?actor ex:zipcode ?postalcode .
        ?actor ex:city ?city .
      }
    """)

  //val writer = new FileWriter("data/coordinates.nt")

  for(result <- resultSet.toList.take(20)) {
    def get(name: String) = URLEncoder.encode(result.getLiteral(name).getString, "utf8")

    val street = get("street")
    val city = get("city")
    //TODO val postalcode = get("postalcode")

    println(result.getLiteral("street").getString)

    val url = s"http://nominatim.openstreetmap.org/search?format=xml" +
              s"&street=$street&city=$city"

    val xml = XML.load(new URL(url))
    val places = xml \ "place"

    if(places.isEmpty) {
      println(s"No coordinates found for $street in $city")
    } else {
      val lat = (places.head \ "@lat").text.toDouble
      val lon = (places.head \ "@lon").text.toDouble

      println(s"$lat - $lon")
    }

    Thread.sleep(1000)
  }



  //writer.close()


//
//  a       ex:Actor ;
//  ex:city "Preu�isch Oldendorf" ;
//  ex:comment "" , "Wieruszewska,Iwona | ?+48-61-871-4193 | iwona.wieruszewska/vw-poznan.pl" ;
//  ex:country "D" ;
//  ex:name "Schwarz GmbH & Co. KG" ;
//  ex:street "Lerchenweg 43" ;
//  ex:zipcode "32361" .
}
