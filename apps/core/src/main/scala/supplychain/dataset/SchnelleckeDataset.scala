package supplychain.dataset

import com.hp.hpl.jena.rdf.model.ModelFactory
import java.io.{File, FileInputStream}
import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryFactory}
import scala.collection.JavaConversions._
import java.util.logging.Logger
import supplychain.model._
import supplychain.model.Connection
import supplychain.model.Coordinates
import supplychain.model.Supplier
import supplychain.model.Address

//TODO should be moved into a separate private module
class SchnelleckeDataset extends Dataset {

  private val log = Logger.getLogger(getClass.getName)

  // Create new model
  private val model = ModelFactory.createDefaultModel()

  // Read data
  val dataDir = "dashboard/data/"
  model.read(new FileInputStream(new File(dataDir + "ontology.ttl")), null, "Turtle")
  model.read(new FileInputStream(new File(dataDir + "avi_fbr.ttl")), null, "Turtle")
  model.read(new FileInputStream(new File(dataDir + "coordinates.ttl")), null, "Turtle")
  //TODO model should be closed

  // All known suppliers
  val suppliers = retrieveSuppliers()

  // All deliveries
  val connections = retrieveDeliveries()

  val messages = List[Message]()

  /**
   * Executes a SPARQL select query.
   */
  def query(queryStr: String) = {
    val query = QueryFactory.create(queryStr)
    QueryExecutionFactory.create(query, model).execSelect()
  }

  override def describe(queryStr: String) = {
    val query = QueryFactory.create(queryStr)
    QueryExecutionFactory.create(query, model).execDescribe()
  }

  /**
   * Retrieves a delivery by id.
   */
  private def retrieveDelivery(id: String): Connection = queryDeliveries(Some(id)) match {
    case Seq(delivery) => delivery
    case _ => throw new Exception(s"Delivery with the ID $id not found.")
  }

  /**
   * Retrieves all deliveries.
   */
  private def retrieveDeliveries(): Seq[Connection] = queryDeliveries(None)

  /**
   * Retrieves all addresses.
   */
  private def retrieveSuppliers(): Seq[Supplier] = {
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
      Supplier(
        uri = result.getResource("sender").getURI,
        name = result.getLiteral("name").getString,
        Address(
          street = result.getLiteral("street").getString,
          zipcode = result.getLiteral("zipcode").getString,
          city = result.getLiteral("city").getString,
          country = result.getLiteral("country").getString),
        Coordinates(
          lat = result.getLiteral("lat").getDouble,
          lon = result.getLiteral("lon").getDouble),
        Product("")
      )
    }
  }

  /**
   * Queries for deliveries.
   * Internal function used by retrieveDelivery() and retrieveDeliveries().
   *
   * @param id If provided, only queries for the delivery with the given id. Otherwise, all deliveries are returned.
   */
  private def queryDeliveries(id: Option[String]): Seq[Connection] = {
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
      case Some(identifier) => query(queryStr.replaceAll("\\?delivery", Namespaces.connection + identifier)).toSeq
      case None => query(queryStr).toSeq
    }
    log.info(s"Retrieved ${resultSet.size} deliveries.")

    // Extract result
    for(result <- resultSet) yield {
      Connection(
        uri = id match {
          case Some(identifier) => Namespaces.connection + identifier
          case None => result.getResource("delivery").getURI
        },
        //date = result.getLiteral("date").getString,
        content = Product(result.getLiteral("content").getString),
        //count = result.getLiteral("count").getInt,
        //unloadingPoint = result.getLiteral("unloadingPoint").getString,
        source =
            Supplier(
              uri = result.getResource("sender").getURI,
              name = result.getLiteral("sname").getString,
              Address(
                street = result.getLiteral("sstreet").getString,
                zipcode = result.getLiteral("szipcode").getString,
                city = result.getLiteral("scity").getString,
                country = result.getLiteral("scountry").getString
              ),
              Coordinates(
                lat = result.getLiteral("slat").getDouble,
                lon = result.getLiteral("slon").getDouble
              ),
              Product("")
            ),
        target =
            Supplier(
              uri = result.getResource("receiver").getURI,
              name = result.getLiteral("rname").getString,
              Address(
                street = result.getLiteral("rstreet").getString,
                zipcode = result.getLiteral("rzipcode").getString,
                city = result.getLiteral("rcity").getString,
                country = result.getLiteral("rcountry").getString
              ),
              Coordinates(
                lat = result.getLiteral("rlat").getDouble,
                lon = result.getLiteral("rlon").getDouble
              ),
              Product("")
            ),
        wsSource = null,
        wsTarget = null
      )
    }
  }
}
