package models

import com.hp.hpl.jena.rdf.model.ModelFactory
import java.io.{File, FileInputStream}
import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryFactory}

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
}
