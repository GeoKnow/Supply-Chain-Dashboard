package supplychain.dataset

import java.util.logging.Logger

import com.hp.hpl.jena.query.QuerySolution
import supplychain.model._

import scala.collection.JavaConversions._

class RdfWeatherDataset(endpointUrl: String, defaultGraph: String) {

  private val log = Logger.getLogger(getClass.getName)

  private val endpoint =
    if (endpointUrl != null && !endpointUrl.trim.isEmpty)
      new RemoteEndpoint(endpointUrl, defaultGraph)
    else
      new LocalEndpoint(defaultGraph)

  private var graphCreated = false

  /**
   * Executes a SPARQL Select query on the data set.
   */
  def select(queryStr: String) = {
    endpoint.select(queryStr)
  }

  /**
   * Executes a SPARQL Describe query on the data set.
   */
  def describe(queryStr: String) = {
    endpoint.describe(queryStr)
  }
}
