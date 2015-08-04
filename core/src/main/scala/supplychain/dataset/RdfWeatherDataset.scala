package supplychain.dataset

import java.util.logging.Logger

import com.hp.hpl.jena.query.QuerySolution
import supplychain.model._

import scala.collection.JavaConversions._

class RdfWeatherDataset(endpoint: Endpoint, defaultGraph: String) {

  private val log = Logger.getLogger(getClass.getName)

  private var graphCreated = false

  /**
   * Executes a SPARQL Select query on the data set.
   */
  /*
  def select(queryStr: String) = {
    endpoint.select(queryStr)
  }
  */

  /**
   * Executes a SPARQL Describe query on the data set.
   */
  def describe(queryStr: String) = {
    endpoint.describe(queryStr)
  }
}
