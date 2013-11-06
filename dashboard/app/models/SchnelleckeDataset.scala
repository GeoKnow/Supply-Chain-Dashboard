package models

import com.hp.hpl.jena.rdf.model.ModelFactory
import java.io.{File, FileInputStream}
import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryFactory}
import play.api.Logger
import scala.collection.JavaConversions._

class SchnelleckeDataset extends Dataset {

  // Create new model
  private val model = ModelFactory.createDefaultModel()

  // Read data
  model.read(new FileInputStream(new File("data/ontology.ttl")), null, "Turtle")
  model.read(new FileInputStream(new File("data/avi_fbr.ttl")), null, "Turtle")
  model.read(new FileInputStream(new File("data/coordinates.ttl")), null, "Turtle")
  //TODO model should be closed

  // All known addresses
  val addresses = retrieveAddresses()

  // All deliveries
  val deliveries = retrieveDeliveries()

  /**
   * Executes a SPARQL select query.
   */
  def query(queryStr: String) = {
    val query = QueryFactory.create(queryStr)
    QueryExecutionFactory.create(query, model).execSelect()
  }

  /**
   * Retrieves a delivery by id.
   */
  private def retrieveDelivery(id: String): Delivery = queryDeliveries(Some(id)) match {
    case Seq(delivery) => delivery
    case _ => throw new Exception(s"Delivery with the ID $id not found.")
  }

  /**
   * Retrieves all deliveries.
   */
  private def retrieveDeliveries(): Seq[Delivery] = queryDeliveries(None)

  /**
   * Retrieves all addresses.
   */
  private def retrieveAddresses(): Seq[Address] = {
    // Build query string
    val queryStr =
      """
       PREFIX sc: <http://geoknow.eu/wp5/ontology#>
       PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
       SELECT * WHERE {
         ?sender a sc:Actor .
         ?sender sc:name ?name .
         ?sender sc:street ?street .
         ?sender sc:zipcode ?zipcode .
         ?sender sc:city ?city .
         ?sender sc:country ?country .
         ?sender geo:lat ?lat .
         ?sender geo:lon ?lon .
       }
      """

    //Execute query
    val resultSet = query(queryStr).toSeq

    // Extract result
    for(result <- resultSet) yield {
      Address(
        uri = result.getResource("sender").getURI,
        name = result.getLiteral("name").getString,
        street = result.getLiteral("street").getString,
        zipcode = result.getLiteral("zipcode").getString,
        city = result.getLiteral("city").getString,
        country = result.getLiteral("country").getString,
        latitude = result.getLiteral("lat").getDouble,
        longitude = result.getLiteral("lon").getDouble
      )
    }
  }

  /**
   * Queries for deliveries.
   * Internal function used by retrieveDelivery() and retrieveDeliveries().
   *
   * @param id If provided, only queries for the delivery with the given id. Otherwise, all deliveries are returned.
   */
  private def queryDeliveries(id: Option[String]): Seq[Delivery] = {
    // Build query string
    val queryStr =
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
      """

    // Either query for all deliveries or just for a specific one.
    val resultSet = id match {
      case Some(identifier) => query(queryStr.replaceAll("\\?delivery", s"<http://geoknow.eu/wp5/message/$identifier>")).toSeq
      case None => query(queryStr).toSeq
    }
    Logger.info(s"Retrieved ${resultSet.size} deliveries.")

    // Extract result
    for(result <- resultSet) yield {
      Delivery(
        uri = id match {
          case Some(identifier) => s"http://geoknow.eu/wp5/message/$identifier"
          case None => result.getResource("delivery").getURI
        },
        date = result.getLiteral("date").getString,
        content = result.getLiteral("content").getString,
        count = result.getLiteral("count").getInt,
        unloadingPoint = result.getLiteral("unloadingPoint").getString,
        sender =
            Address(
              uri = result.getResource("sender").getURI,
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
              uri = result.getResource("receiver").getURI,
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
