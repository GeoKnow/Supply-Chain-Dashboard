package supplychain.dataset

import java.util.UUID
import java.util.logging.Logger

import com.hp.hpl.jena.query.QuerySolution
import supplychain.metric.Metrics
import supplychain.model._

import scala.collection.JavaConversions._

class MetricsDataset(ec: EndpointConfig, defaultGraph: String) {

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
   * Adds a connection to the RDF data set.
   */
  def addMetricValue(messages: Seq[Message], supplier: Supplier, date: DateTime) {

      val uri = defaultGraph + "metrics/" + UUID.randomUUID().toString

      val metricsValues = for(m <- Metrics.all) yield {
        val value = m.apply(messages)
        s"""<$uri> sc:metric${m.getClass.getSimpleName} "$value"^^xsd:double ."""
      }

      val queryString =
        s"""
           | <$uri> a qb:Observation .
           | <$uri> qb:dataSet <${defaultGraph + "PerformanceMetrics"}> .
           | <$uri> sc:supplier <${supplier.uri}> .
           | <$uri> sc:date "${date.toXSDFormat}"^^xsd:date .
           | ${metricsValues.mkString("\n")}
         """.stripMargin
      insert(queryString)
  }

  /**
   * Inserts a number of statements into the RDF data set.
   */
  private def insert(statements: String) {
    if(!graphCreated) {
      defaultGraph
      ec.createEndpoint().update(s"CREATE SILENT GRAPH <${defaultGraph}>")
      graphCreated = true
    }

    val query =
      s"""
         | PREFIX sc: <${Namespaces.schema}>
         | PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
         | PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         | PREFIX qb: <http://purl.org/linked-data/cube#>
         | PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
         | INSERT DATA {
         |   GRAPH <${ec.getDefaultGraph()}> {
         |     $statements
         |   }
         | }
      """.stripMargin
    ec.createEndpoint().update(query)
  }

  /**
   * Executes a SPARQL Describe query on the data set.
   */
  def describe(queryStr: String) = {
    ec.createEndpoint().describe(queryStr)
  }
}
