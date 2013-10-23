package models

import com.hp.hpl.jena.rdf.model.ModelFactory
import play.api.Play
import play.api.Play.current
import java.io.FileInputStream
import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryFactory}

/**
 * Holds the supply chain dataset and allows to query it.
 */
object Dataset {

  //Create new model
  private val model = ModelFactory.createDefaultModel()

  //Read data
  model.read(new FileInputStream(Play.getFile("data/ontology.ttl")), null, "Turtle")
  model.read(new FileInputStream(Play.getFile("data/avi_fbr.ttl")), null, "Turtle")

  /**
   * Executes a SPARQL select query.
   */
  def query(queryStr: String) = {
    val query = QueryFactory.create(queryStr)
    QueryExecutionFactory.create(query, model).execSelect()
  }
}
