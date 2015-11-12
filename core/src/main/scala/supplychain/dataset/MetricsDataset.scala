package supplychain.dataset

import java.util.logging.Logger

import supplychain.metric.{SilkMetric, Metric, Metrics}
import supplychain.model._

import scala.collection.mutable.ListBuffer

class MetricsDataset(ec: EndpointConfig, silkProject: String, simulationStartDate: DateTime, simulationEndDate: DateTime) {

  private val log = Logger.getLogger(getClass.getName)

  private var graphCreated = false

  private val metrics = Metrics.all // ++ SilkMetrics.load(silkProject)

  private val dataSetUri = ec.getDefaultGraphMetrics() + "PerformanceMetricsDataSet"
  private val dataStructureUri = ec.getDefaultGraphMetrics() + "PerformanceMetricsDataStructure"
  private val wp = new WeatherProvider(ec)

  /**
   * Executes a SPARQL Select query on the data set.
   */
  /*
  def select(queryStr: String) = {
    endpoint.select(queryStr)
  }
  */

  def generateDataSet(): Unit = {

    val uri1 = generateUri(suffix="component-supplier")
    val uri2 = generateUri(suffix="component-date")
    val uri3 = generateUri(suffix="component-area")

      var queryString =
      s"""
          | <${dataStructureUri}> a qb:DataStructureDefinition ;
          |     qb:component <$uri1> ;
          |     qb:component <$uri2> ;
          |     qb:component <$uri3> .
          | <$uri1> qb:dimension sc:supplier ;
          |     qb:order 1 .
          | <$uri2> qb:dimension sc:date ;
          |     qb:order 2 .
          | <$uri3> qb:dimension sc:refArea ;
          |     qb:order 3 .
          |
          | sc:supplier a rdf:Property, qb:DimensionProperty ;
          |    rdfs:label "Supplier"@en ;
          |    rdfs:range sc:Supplier .
          |
          | sc:date a  rdf:Property, qb:DimensionProperty ;
          |    rdfs:label "Observation Date"@en ;
          |    rdfs:subPropertyOf sdmx-dimension:refTime ;
          |    rdfs:range xsd:date .
          |
          | sc:refArea a rdf:Property, qb:DimensionProperty ;
          |    rdfs:label "reference area" @en ;
          |    rdfs:subPropertyOf sdmx-dimension:refArea ;
          |    rdfs:range sc:Area, ogcgs:Feature ;
          |    qb:concept sdmx-concept:refArea .
          |
          | <${dataSetUri}> a qb:DataSet ;
          |    qb:structure <${dataStructureUri}> .
          """.stripMargin

    for (m <- metrics) {
      val uri = generateUri(suffix="component"+getMetricProperty(m))
      queryString +=
      s"""
          | <${dataStructureUri}> qb:component <$uri> .
          | <$uri> qb:measure sc:metric${getMetricProperty(m)} .
          """.stripMargin

      queryString +=
      s"""
         | sc:metric${getMetricProperty(m)} a rdf:Property, qb:MeasureProperty ;
         |    rdfs:label "${m.dimension}"@en ;
         |    rdfs:subPropertyOf sdmx-measure:obsValue ;
         |    rdfs:range xsd:double .
       """.stripMargin
    }

    for (m <- List("tmin", "tmax", "prcp", "snwd")) {
      val uri = generateUri(suffix="component_" + m)
      queryString +=
        s"""
           | <${dataStructureUri}> qb:component <$uri> .
           | <$uri> qb:measure sc:metric_${m} .
          """.stripMargin

      queryString +=
        s"""
           | sc:metric_${m} a rdf:Property, qb:MeasureProperty ;
           |    rdfs:label "${m}"@en ;
           |    rdfs:subPropertyOf sdmx-measure:obsValue ;
           |    rdfs:range xsd:double .
       """.stripMargin
    }

    insert(queryString)
  }

  private def getMetricProperty(m: Metric): String = {
    m match {
      case as: SilkMetric => {"_DI_" + m.dimension.filter(_.isLetterOrDigit)}
      case as: Metric => {"_" + m.dimension.filter(_.isLetterOrDigit)}
    }
  }

    /**
   * Adds a connection to the RDF data set.
   */
  def addMetricValue(messages: Seq[Message], supplier: Supplier, date: DateTime) {

      var listBuffer = new ListBuffer[String]()

      val obsUri = generateUri(suffix="obs-" + supplier.id + "-" + date.toXSDFormat)
      var metricsValues = for(m <- metrics) yield {
        val value = m.apply(messages, date)
        val msg = s"""<$obsUri> sc:metric${getMetricProperty(m)} "$value"^^xsd:double ."""
        log.info(msg)
        msg
      }

      log.info("getDailySummary for : " + date.toYyyyMMdd())
      val wObs = wp.getDailySummary(supplier.weatherStation, date, simulationStartDate, simulationEndDate)
      if (wObs != null) {
        listBuffer += s"""<$obsUri> sc:metric_tmin "${wObs.tmin}"^^xsd:double ."""
        listBuffer += s"""<$obsUri> sc:metric_tmax "${wObs.tmax}"^^xsd:double ."""
        listBuffer += s"""<$obsUri> sc:metric_prcp "${wObs.prcp}"^^xsd:double ."""
        listBuffer += s"""<$obsUri> sc:metric_snwd "${wObs.snwd}"^^xsd:double ."""
      }

      listBuffer.prependToList(metricsValues)

      var queryString =
        s"""
           | <$obsUri> a qb:Observation .
           | <$obsUri> qb:dataSet <${dataSetUri}> .
           | <$obsUri> sc:supplier <${supplier.uri}> .
           | <$obsUri> sc:date "${date.toXSDFormat}"^^xsd:date .
           |
           | ${listBuffer.toList.mkString("\n")}
         """.stripMargin
      if (supplier.feature.length > 0) {
        queryString =
          s"""
             | ${queryString}
             | <$obsUri> sc:refArea <${supplier.feature}> .
           """.stripMargin
      }
      insert(queryString)
  }

  private def generateUri(suffix: String = "") = {
    //val pref = prefix.getOrElse("")
    ec.getDefaultGraphMetrics() + "" + suffix// + UUID.randomUUID().toString
  }

  def normalizeDataCube(): Unit = {
    var i = s"""
        |    ?o  rdf:type qb:Observation .
        |    ?ds rdf:type qb:DataSet .
        """.stripMargin
    var w = "?o qb:dataSet ?ds ."
    insertWhere(i,w)

    i = s"""
         |    ?cs qb:componentProperty ?p .
         |    ?p  rdf:type qb:DimensionProperty .
        """.stripMargin
    w = "?cs qb:dimension ?p ."
    insertWhere(i,w)

    i = s"""
         |    ?cs qb:componentProperty ?p .
         |    ?p  rdf:type qb:MeasureProperty .
        """.stripMargin
    w = "?cs qb:measure ?p ."
    insertWhere(i,w)

    i = s"""
         |    ?cs qb:componentProperty ?p .
         |    ?p  rdf:type qb:AttributeProperty .
        """.stripMargin
    w = "?cs qb:attribute ?p ."
    insertWhere(i,w)
  }

  private def insertWhere(insertStm: String, whereStm: String): Unit = {
    val query =
      s"""
        |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        |PREFIX qb: <http://purl.org/linked-data/cube#>
        |
        |WITH <${ec.getDefaultGraphMetrics()}>
        |INSERT {
        |   $insertStm
        |} WHERE {
        |   $whereStm
        |}
      """.stripMargin
    ec.getEndpoint().update(query)
  }

  /**
   * Inserts a number of statements into the RDF data set.
   */
  private def insert(statements: String) {
    if(!graphCreated) {
      ec.getEndpoint().update(s"CREATE SILENT GRAPH <${ec.getDefaultGraphMetrics()}>")
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
         | PREFIX ogcgs: <http://www.opengis.net/ont/geosparql#>
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
    ec.getEndpoint().update(query)
  }

  /**
   * Executes a SPARQL Describe query on the data set.
   */
  def describe(queryStr: String) = {
    ec.getEndpoint().describe(queryStr)
  }
}
