package supplychain.dataset

import java.util.logging.Logger

import com.hp.hpl.jena.query.{QuerySolution, QueryExecutionFactory}
import supplychain.metric.{Metric, SilkMetrics, Metrics, MetricValue}
import supplychain.model._
import scala.collection.JavaConversions._

class RdfDataset(ec: EndpointConfig, silkProject: String) {

  private val log = Logger.getLogger(getClass.getName)

  private var graphCreated = false

  private val internalMetrics = Metrics.all
  private val silkMetrics = SilkMetrics.load(silkProject)

  /**
   * Adds a product to the RDF data set.
   */
  def addProduct(product: Product) {
    val properties =
      s"""
       | <${product.uri}> a sc:Product ;
       |                   sc:name "${product.name}" ;
       |                   sc:count "${product.count}" ;
       """

    val parts =
      for(part <- product.parts) yield
        s"|                   sc:part <${part.uri}> ;"

    var statements = properties + parts.mkString("\n")
    statements = statements.updated(statements.lastIndexOf(';'), '.')
    insert(statements)

    for(part <- product.parts)
      addProduct(part)
  }

  /**
   * Adds a supplier to the RDF data set.
   */
  def addSupplier(supplier: Supplier) {
     insert(s"""
     | <${supplier.uri}> a sc:Supplier ;
     |                   sc:name "${supplier.name}" ;
     |                   rdfs:label "${supplier.name}" ;
     |                   sc:street "${supplier.address.street}" ;
     |                   sc:zipcode "${supplier.address.zipcode}" ;
     |                   sc:city "${supplier.address.city}" ;
     |                   sc:product <${supplier.product.uri}> ;
     |                   geo:lat "${supplier.coords.lat}" ;
     |                   geo:long "${supplier.coords.lon}" ;
     |                   sc:weatherStation <${supplier.weatherStation.uri}> .
     """)
  }

  /**
   * Adds a connection to the RDF data set.
   */
  def addConnection(c: Connection) {
    insert(s"""
     | <${c.uri}> a sc:Connection ;
     |            sc:product <${c.content.uri}> ;
     |            sc:sender <${c.source.uri}> ;
     |            sc:receiver <${c.target.uri}> .
     """)
  }

  /**
   * Adds a new message to the RDF data set.
   */
  def addMessage(msg: Message) { msg match {
    case order @ Order(uri, date, connection, count) =>
      insert(s"""
        |  <${msg.uri}> a sc:Order ;
        |               sc:date "${date.toXSDFormat}" ;
        |               sc:dueDate "${order.dueDate.toXSDFormat}" ;
        |               sc:connection <${connection.uri}> ;
        |               sc:count "$count" .
        """)
      log.info("Order date: " + date.toXSDFormat)

    case Shipping(uri, date, connection, count, order) =>
      insert(s"""
        |  <${msg.uri}> a sc:Shipping ;
        |               sc:date "${date.toXSDFormat}" ;
        |               sc:connection <${connection.uri}> ;
        |               sc:count "$count" ;
        |               sc:order <${order.uri}> .
        """)
      log.info("Shipping date: " + date.toXSDFormat)
  }}

  /*
   * get the metrics from the runtime graph
   */
  def getMetrics(date: DateTime, supplier: Supplier): Seq[MetricValue] = {
    val queryStr =
      s"""
        |SELECT DISTINCT ?propVal ?obsProp WHERE {
        |  ?obs a qb:Observation .
        |  ?obs sc:supplier <${supplier.uri}> .
        |  ?obs sc:date "${date.toYyyyMMdd()}Z"^^xsd:date .
        |  ?obs ?obsProp ?propVal .
        |  ?obsProp a qb:MeasureProperty .
        |  ?obsProp rdfs:label ?propLabel .
        |}
      """.stripMargin

    val prefixedQS = prefix(queryStr)
    //log.info(prefixedQS)
    val result = ec.getEndpoint().select(prefixedQS).toSeq
    for (b <- result; metric <- getMetricFromUri(b.getResource("obsProp").getURI)) yield {
      MetricValue(
        metric = metric,
        value = b.getLiteral("propVal").getDouble
      )
    }
  }

  private def getMetricFromUri(uri: String): Option[Metric] = {
    if (uri.contains("_DI_")) {
      silkMetrics.find(m => uri.endsWith(m.dimension.filter(_.isLetterOrDigit)))
    } else {
      internalMetrics.find(m => uri.endsWith(m.dimension.filter(_.isLetterOrDigit)))
    }
  }

  /*
   * get messages from the runtime graph
   */
  def getMessages(start: DateTime, end: DateTime, connections: Seq[Connection]): Seq[Message] = {
    var offset: Int = 0
    var msgs = getMessagesOffset(start, end, connections, offset * 1000)
    var messages: Seq[Message] = List()
    while (!msgs.isEmpty) {
      messages ++= msgs
      offset += 1
      msgs = getMessagesOffset(start, end, connections, offset * 1000)
    }

    messages = messages.sortBy(_.uri)

    log.info("#############################")
    log.info("# of messages: " + messages.size.toString)
    log.info("# messages.hashCode: " + messages.hashCode().toString)
    log.info("# messages.hashCode: " + messages.hashCode().toString)
    log.info(messages.mkString("\n"))

    log.info("#############################")

    messages
  }

  /*
   * get messages from the runtime graph
   */
  private def getMessagesOffset(start: DateTime, end: DateTime, connections: Seq[Connection], offset: Int): Seq[Message] = {

    val startDate = start.toFormat("yyyy-MM-dd")
    val endDate = end.toFormat("yyyy-MM-dd")

    val queryStr =
      s"""
        |SELECT DISTINCT ?msg ?date ?dueDate ?connection ?count ?order ?orderDate ?orderDueDate ?orderConnection ?orderCount FROM <${ec.getDefaultGraph()}>
        |WHERE {
        |   ?msg sc:date ?date .
        |   OPTIONAL { ?msg sc:dueDate ?dueDate . }
        |   OPTIONAL {
        |     ?msg sc:order ?order .
        |     ?order sc:date ?orderDate .
        |     ?order sc:dueDate ?orderDueDate .
        |     ?order sc:connection ?orderConnection .
        |     ?order sc:count ?orderCount .
        |   }
        |   ?msg sc:connection ?connection .
        |   ?msg sc:count ?count .
        |   FILTER ( str(?date) >= "$startDate" )
        |   FILTER ( str(?date) <= "$endDate" )
        |} LIMIT 1000 OFFSET ${offset.toString}
      """.stripMargin

    val prefixedQS = prefix(queryStr)
    //log.info(prefixedQS)
    val result = ec.getEndpoint().select(prefixedQS).toSeq
    var messages: List[Message] = List()

    for (binding <- result) {
      val conn = connections.find(_.uri == binding.getResource("connection").getURI)
      if (conn.isDefined) {
        if (binding.contains("dueDate")) {
          // -> Order
          val date = DateTime.parse(binding.getLiteral("date").getString)
          val order = new Order(
            uri = binding.getResource("msg").getURI,
            date = date,
            connection = conn.get,
            count = binding.getLiteral("count").getString.toInt
          )
          messages = order :: messages
        } else if (binding.contains("order")) {
          // -> Shipping
          val date = DateTime.parse(binding.getLiteral("date").getString)
          val orderDate = DateTime.parse(binding.getLiteral("orderDate").getString)
          val order = Order(
            uri = binding.getResource("order").getURI,
            date = orderDate,
            connection = conn.get,
            count = binding.getLiteral("orderCount").getString.toInt
          )
          val shipping = new Shipping(
            uri = binding.getResource("msg").getURI,
            date = date,
            connection = conn.get,
            count = binding.getLiteral("count").getString.toInt,
            order = order
          )
          messages = shipping :: messages
        } else {
          throw new Exception("Unkown message type.")
        }
      }
    }
    messages.toSeq
  }

  /**
   * Inserts a number of statements into the RDF data set.
   */
  private def insert(statements: String) {
    if(!graphCreated) {
      ec.getDefaultGraph()
      ec.getEndpoint().update(s"CREATE SILENT GRAPH <${ec.getDefaultGraph()}>")
      graphCreated = true
    }

    val query =
      s"""
        | PREFIX sc: <${Namespaces.schema}>
        | PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
        | PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        | INSERT DATA {
        |   GRAPH <${ec.getDefaultGraph()}> {
        |     $statements
        |   }
        | }
      """.stripMargin
    ec.getEndpoint().update(query)
  }

  private def prefix(query: String): String = {
    s"""
       |PREFIX sc: <http://www.xybermotive.com/ontology/>
       |PREFIX prod: <http://www.xybermotive.com/products/>
       |PREFIX dbpedia: <http://dbpedia.org/resource/>
       |PREFIX suppl: <http://www.xybermotive.com/supplier/>
       |PREFIX schema: <http://schema.org/>
       |PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
       |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
       |PREFIX qb: <http://purl.org/linked-data/cube#>
       |
       |${query}
      """.stripMargin
  }

  /**
   * Executes a SPARQL Select query on the data set.
   */
  def query(queryStr: String) = {
    ec.getEndpoint().select(queryStr)
  }

  /**
   * Executes a SPARQL Select query on the data set.
   */
  def select(queryStr: String): Seq[QuerySolution] = {
    return ec.getEndpoint().select(queryStr).toSeq
  }

  /**
   * Executes a SPARQL Describe query on the data set.
   */
  def describe(queryStr: String) = {
    ec.getEndpoint().describe(queryStr)
  }

}
