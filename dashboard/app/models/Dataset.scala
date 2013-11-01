package models

import com.hp.hpl.jena.rdf.model.ModelFactory
import java.io.{File, FileInputStream}
import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryFactory}
import scala.collection.JavaConversions._
import play.api.Logger

/**
 * Holds the supply chain dataset and allows to query it.
 */
object Dataset {

  //Create new model
  private val model = ModelFactory.createDefaultModel()

  //Read data
  model.read(new FileInputStream(new File("data/ontology.ttl")), null, "Turtle")
  model.read(new FileInputStream(new File("data/avi_fbr.ttl")), null, "Turtle")
  model.read(new FileInputStream(new File("data/coordinates.ttl")), null, "Turtle")

  /**
   * Executes a SPARQL select query.
   */
  def query(queryStr: String) = {
    val query = QueryFactory.create(queryStr)
    QueryExecutionFactory.create(query, model).execSelect()
  }

  def queryDeliveries(): Seq[Delivery] = {
    val resultSet = query(
      """
        PREFIX sc: <http://geoknow.eu/wp5/ontology#>
        PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
        SELECT * WHERE {
          ?delivery a sc:Message .
          ?delivery sc:contentType ?content .
          ?delivery sc:count ?count .
          ?delivery sc:date ?date .
          ?delivery sc:unloadingPoint ?unloadingPoint .

          ?delivery sc:sender ?sender .
          ?sender sc:name ?sname .
          ?sender sc:street ?sstreet .
          ?sender sc:zipcode ?szipcode .
          ?sender sc:city ?scity .
          ?sender sc:country ?scountry .
          ?sender geo:lat ?slat .
          ?sender geo:lon ?slon .

          ?delivery sc:receiver ?receiver .
          ?receiver sc:name ?rname .
          ?receiver sc:street ?rstreet .
          ?receiver sc:zipcode ?rzipcode .
          ?receiver sc:city ?rcity .
          ?receiver sc:country ?rcountry .
          ?receiver geo:lat ?rlat .
          ?receiver geo:lon ?rlon .
        }
      """).take(10).toSeq

    Logger.info(s"Found ${resultSet.size} deliveries.")

    for(result <- resultSet) yield {
      Delivery(
        uri = result.getResource("delivery").getURI,
        date = result.getLiteral("date").getString,
        content = result.getLiteral("content").getString,
        count = result.getLiteral("count").getInt,
        unloadingPoint = result.getLiteral("unloadingPoint").getString,
        sender =
          Address(
            name = result.getLiteral("sname").getString,
            street = result.getLiteral("sstreet").getString,
            zipcode = result.getLiteral("szipcode").getString,
            city = result.getLiteral("scity").getString,
            country = result.getLiteral("scountry").getString,
            latitude = result.getLiteral("slat").getDouble,
            longitude = result.getLiteral("slon").getDouble
          ),
        receiver =
          Address(
            name = result.getLiteral("rname").getString,
            street = result.getLiteral("rstreet").getString,
            zipcode = result.getLiteral("rzipcode").getString,
            city = result.getLiteral("rcity").getString,
            country = result.getLiteral("rcountry").getString,
            latitude = result.getLiteral("rlat").getDouble,
            longitude = result.getLiteral("rlon").getDouble
          )
      )
    }
  }
}
