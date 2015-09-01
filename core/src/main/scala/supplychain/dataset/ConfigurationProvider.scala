package supplychain.dataset

import java.util.logging.Logger

import supplychain.model._

import scala.collection.JavaConversions._

/**
 * Created by rene on 09.01.15.
 */
class ConfigurationProvider(epc: EndpointConfig, wp: WeatherProvider, productUri: String) {

  private val log = Logger.getLogger(getClass.getName)

  private def prefix(query: String): String = {
    s"""
       |PREFIX sc: <http://www.xybermotive.com/ontology/>
       |PREFIX prod: <http://www.xybermotive.com/products/>
       |PREFIX dbpedia: <http://dbpedia.org/resource/>
       |PREFIX suppl: <http://www.xybermotive.com/supplier/>
       |PREFIX schema: <http://schema.org/>
       |PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
       |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
       |
       |${query}
      """.stripMargin
  }

  /*
   * get messages from the runtime graph
   */
  def getMessages(start: DateTime, end: DateTime, connections: Seq[Connection]): Seq[Message] = {

    val startDate = start.toFormat("yyyy-MM-dd")
    val endDate = end.toFormat("yyyy-MM-dd")

    val queryStr =
      s"""
        |SELECT DISTINCT ?msg ?date ?dueDate ?connection ?count ?order ?orderDate ?orderDueDate ?orderConnection ?orderCount FROM <${epc.getDefaultGraph()}>
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
        |
        |}
      """.stripMargin

    val prefixedQS = prefix(queryStr)
    //log.info(prefixedQS)
    val result = epc.getEndpoint().select(prefixedQS).toSeq
    var messages: List[Message] = List()

    for (binding <- result) {
      val conn = connections.find(_.uri == binding.getResource("connection").getURI)
      if (conn.isDefined) {
        if (binding.contains("dueDate")) {
          // -> Order
          val date = DateTime.parse(binding.getLiteral("date").getString)
          val order = new Order(
            //uri = binding.getResource("msg").getURI,
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
            //uri = binding.getResource("order").getURI,
            date = orderDate,
            connection = conn.get,
            count = binding.getLiteral("orderCount").getString.toInt
          )
          val shipping = new Shipping(
            //uri = binding.getResource("msg").getURI,
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

  /*
   * get connections from the runtime graph
   */
  def getConnections(supplier: Seq[Supplier]): Seq[Connection] = {
    val supplierMap = supplier.groupBy(_.uri).mapValues(_.head)
    val queryStr =
      s"""
        |SELECT DISTINCT ?uri ?product ?sender ?receiver FROM <${epc.getDefaultGraph()}>
        |WHERE {
        |  ?uri a sc:Connection .
        |  ?uri sc:product ?product .
        |  ?uri sc:sender ?sender .
        |  ?uri sc:receiver ?receiver .
        |}
      """.stripMargin

    val prefixedQS = prefix(queryStr)
    //log.info(prefixedQS)
    val result = epc.getEndpoint().select(prefixedQS).toSeq

    var connections: List[Connection] = List()

    for (binding <- result) {
      val senderUri = binding.getResource("sender").getURI
      val receiverUri = binding.getResource("receiver").getURI
      if (supplier.find(_.uri == senderUri).isDefined && supplier.find(_.uri == receiverUri).isDefined) {
        val product = getProduct(binding.getResource("product").getURI)

        val c = new Connection(
          uri = binding.getResource("uri").getURI,
          product,
          source = supplierMap(senderUri),
          target = supplierMap(receiverUri)
        )
        connections =  c :: connections
      }
    }
    connections.toSeq
  }

  /**
   * returns the product structure for the given product uri or the configured simulation product if omitted
   * read data from the configuration graph
   */
  def getProduct(uri: String = productUri): Product = {

    val queryStr =
      s"""
        |SELECT DISTINCT ?name FROM <${epc.getDefaultGraphConfiguration()}>
        |WHERE {
        |  <${uri}> schema:name ?name .
        |  #FILTER( LANGMATCHES( LANG( ?name ), "DE" ) ) .
        |} LIMIT 1
      """.stripMargin

    val prefixedQS = prefix(queryStr)
    //log.info(prefixedQS)
    val result = epc.getEndpoint().select(prefixedQS).toSeq

    var p: Product = null
    for (binding <- result) {
      val name = binding.getLiteral("name").getString
      p = new Product(name, 1, getParts(uri), uri)
    }
    log.info("# PRODUCT: " + p.toString)
    p
  }

  /*
   * read suppliers from the configuration graph
   */
  def getSupplier(product: Product): Supplier = {
    val queryStr =
      s"""
        |PREFIX suppl: <http://www.xybermotive.com/supplier/>
        |PREFIX schema: <http://schema.org/>
        |PREFIX prod: <http://www.xybermotive.com/products/>
        |
        |SELECT DISTINCT ?suppl ?name ?street ?zip ?city ?country ?long ?lat FROM <${epc.getDefaultGraphConfiguration()}>
        |WHERE {
        |  ?suppl schema:manufacturer <${product.uri}> .
        |  ?suppl schema:legalName ?name .
        |  ?suppl schema:location ?loc .
        |  ?loc schema:address ?adr .
        |  ?loc geo:lat ?lat .
        |  ?loc geo:long ?long .
        |  ?adr schema:streetAddress ?street .
        |  ?adr schema:postalCode ?zip .
        |  ?adr schema:addressLocality ?city .
        |  ?adr schema:addressCountry ?country .
        |} LIMIT 1
      """.stripMargin
    val prefixedQS = prefix(queryStr)
    //log.info(prefixedQS)
    val result = epc.getEndpoint().select(prefixedQS).toSeq

    var s: Supplier = null
    for (binding <- result) {
      val uri = binding.getResource("suppl").getURI
      val name = binding.getLiteral("name").getString
      val street = binding.getLiteral("street").getString
      val zip = binding.getLiteral("zip").getString
      val city = binding.getLiteral("city").getString
      val country = binding.getLiteral("country").getString
      val long = binding.getLiteral("long").getFloat
      val lat = binding.getLiteral("lat").getFloat

      val adr = new Address(street, zip, city, country)
      val coords = new Coordinates(lat, long)
      val ws = wp.getNearesWeaterStation(coords)
      s = new Supplier(uri, name, adr, coords, product, ws)
    }
    log.info("# SUPPLIER: " + s.toString)
    s
  }

  /*
   * read parts from the configuration graph
   */
  private def getParts(uri: String): List[Product] = {
    val queryStr =
      s"""
        |SELECT ?uri ?name ?quantity FROM <${epc.getDefaultGraphConfiguration()}>
        |WHERE {
        |  <${uri}> sc:productPart ?pp .
        |  ?pp sc:product ?uri .
        |  ?pp sc:quantity ?quantity .
        |  ?uri schema:name ?name .
        |  #FILTER( LANGMATCHES( LANG( ?name ), "DE" ) ) .
        |}
       """.stripMargin

    val prefixedQS = prefix(queryStr)
    //log.info(prefixedQS)
    val result = epc.getEndpoint().select(prefixedQS).toSeq

    var pl: List[Product] = List()
    for (binding <- result) {
      val uri = binding.getResource("uri").toString
      val name = binding.getLiteral("name").getString
      val quantity = binding.getLiteral("quantity").getInt

      val p = new Product(name, quantity, getParts(uri), uri)
      pl = p :: pl
    }
    pl
  }
}
