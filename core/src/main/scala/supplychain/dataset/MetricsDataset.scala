package supplychain.dataset

import java.util.UUID
import java.util.logging.Logger

import supplychain.metric.{Metrics}
import supplychain.model._

class MetricsDataset(ec: EndpointConfig) {

  private val log = Logger.getLogger(getClass.getName)

  private var graphCreated = false

  private val metrics = Metrics.all // ++ SilkMetrics.load(Configuration.get.silkProject)

  private val dataSetUri = ec.getDefaultGraphMetrics() + "PerformanceMetricsDataSet"
  private val dataStructureUri = ec.getDefaultGraphMetrics() + "PerformanceMetricsDataStructure"

  /**
   * Executes a SPARQL Select query on the data set.
   */
  /*
  def select(queryStr: String) = {
    endpoint.select(queryStr)
  }
  */

  def generateDataSet(): Unit = {

    val uri1 = generateUri(prefix="component-")
    val uri2 = generateUri(prefix="component-")

      var queryString =
      s"""
          | <${dataStructureUri}> a qb:DataStructureDefinition ;
          |     qb:component <$uri1> ;
          |     qb:component <$uri2> .
          | <$uri1> qb:dimension sc:supplier ;
          |     qb:order 1 .
          | <$uri2> qb:dimension sdmx-dimension:refTime;
          |     qb:order 2 .
          |
          | sc:supplier a rdf:Property, qb:DimensionProperty ;
          |    rdfs:label "Supplier"@en ;
          |    rdfs:range sc:Supplier .
          |
          | <${dataSetUri}> a qb:DataSet ;
          |    qb:structure <${dataStructureUri}> .
          """.stripMargin

    for (m <- Metrics.all) {
      val uri = generateUri(prefix="component-")
      queryString +=
      s"""
          | <${dataStructureUri}> qb:component <$uri> .
          | <$uri1> qb:measure sc:metric${m.getClass.getSimpleName} .
          """.stripMargin

      queryString +=
      s"""
         | sc:metric${m.getClass.getSimpleName} a rdf:Property, qb:MeasureProperty ;
         |    rdfs:label "${m.dimension}"@en ;
         |    rdfs:subPropertyOf sdmx-measure:obsValue ;
         |    rdfs:range xsd:double .
       """.stripMargin
    }

    insert(queryString)
  }

    /**
   * Adds a connection to the RDF data set.
   */
  def addMetricValue(messages: Seq[Message], supplier: Supplier, date: DateTime) {

      val uri = generateUri(prefix="metric-")

      val metricsValues = for(m <- Metrics.all) yield {
        val value = m.apply(messages)
        s"""<$uri> sc:metric${m.getClass.getSimpleName} "$value"^^xsd:double ."""
      }

      val queryString =
        s"""
           | <$uri> a qb:Observation .
           | <$uri> qb:dataSet <${dataSetUri}> .
           | <$uri> sc:supplier <${supplier.uri}> .
           | <$uri> sdmx-dimension:refTime "${date.toXSDFormat}"^^xsd:date .
           | ${metricsValues.mkString("\n")}
         """.stripMargin
      insert(queryString)
  }

  private def generateUri(prefix: String = "") = {
    //val pref = prefix.getOrElse("")
    ec.getDefaultGraphMetrics() + "" + prefix + UUID.randomUUID().toString
  }


  /**
   * Inserts a number of statements into the RDF data set.
   */
  private def insert(statements: String) {
    if(!graphCreated) {
      ec.createEndpoint().update(s"CREATE SILENT GRAPH <${ec.getDefaultGraphMetrics()}>")
      graphCreated = true
    }
    /*
    sdmx-concept	http://purl.org/linked-data/sdmx/2009/concept#	SKOS Concepts for each COG defined concept
    sdmx-code	http://purl.org/linked-data/sdmx/2009/code#	SKOS Concepts and ConceptSchemes for each COG defined code list
    sdmx-dimension	http://purl.org/linked-data/sdmx/2009/dimension#	component properties corresponding to each COG concept that can be used as a dimension
    sdmx-attribute	http://purl.org/linked-data/sdmx/2009/attribute#	component properties corresponding to each COG concept that can be used as an attribute
    sdmx-measure	http://purl.org/linked-data/sdmx/2009/measure#
     */

    val query =
      s"""
         | PREFIX sc: <${Namespaces.schema}>
         | PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
         | PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         | PREFIX sdmx-concept: <http://purl.org/linked-data/sdmx/2009/concept#>
         | PREFIX sdmx-code: <http://purl.org/linked-data/sdmx/2009/code#>
         | PREFIX sdmx-dimension: <http://purl.org/linked-data/sdmx/2009/dimension#>
         | PREFIX sdmx-attribute: <http://purl.org/linked-data/sdmx/2009/attribute#>
         | PREFIX sdmx-measure: <http://purl.org/linked-data/sdmx/2009/measure#>
         | PREFIX qb: <http://purl.org/linked-data/cube#>
         | PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
         | INSERT DATA {
         |   GRAPH <${ec.getDefaultGraphMetrics()}> {
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
