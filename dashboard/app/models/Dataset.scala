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
        PREFIX ex: <http://geoknow.eu/wp5/ontology#>
        PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
        SELECT ?delivery ?slat ?slon ?rlat ?rlon WHERE {
          ?delivery a ex:Message .
          ?delivery ex:sender ?sender .
          ?sender geo:lat ?slat .
          ?sender geo:lon ?slon .
          ?delivery ex:receiver ?receiver .
          ?receiver geo:lat ?rlat .
          ?receiver geo:lon ?rlon .
        }
      """).take(10).toSeq

    Logger.info(s"Found ${resultSet.size} deliveries.")

    for(result <- resultSet) yield {
      Delivery(
        name = result.getResource("delivery").getURI,
        sender = (result.getLiteral("slat").getDouble, result.getLiteral("slon").getDouble),
        receiver = (result.getLiteral("rlat").getDouble, result.getLiteral("rlon").getDouble)
      )
    }
  }
}
