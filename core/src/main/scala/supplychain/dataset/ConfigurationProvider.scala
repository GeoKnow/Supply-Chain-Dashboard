package supplychain.dataset

import java.util.logging.Logger

import supplychain.exceptions.UnknownProductException
import supplychain.model._

import scala.collection.JavaConversions._

/**
 * Created by rene on 09.01.15.
 */
class ConfigurationProvider(epc: EndpointConfig, wp: WeatherProvider) {

  private val log = Logger.getLogger(getClass.getName)

  private def prefix(query: String): String = {
    s"""
       |PREFIX sc:      <http://www.xybermotive.com/ontology/>
       |PREFIX prod:    <http://www.xybermotive.com/products/>
       |PREFIX dbpedia: <http://dbpedia.org/resource/>
       |PREFIX suppl:   <http://www.xybermotive.com/supplier/>
       |PREFIX schema:  <http://schema.org/>
       |PREFIX geo:     <http://www.w3.org/2003/01/geo/wgs84_pos#>
       |PREFIX ogcgs:   <http://www.opengis.net/ont/geosparql#>
       |PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>
       |
       |${query}
      """.stripMargin
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
  def getProduct(uri: String): Product = {

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
    if (p == null) {
      val msg = "No product found for given uri: " + uri
      log.info(msg)
      throw new UnknownProductException(msg)
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
        |SELECT DISTINCT ?suppl ?name ?street ?zip ?city ?country ?long ?lat ?feature
        |FROM <${epc.getDefaultGraphConfiguration()}>
        |FROM <http://linkedgeodata.org/gadm2/>
        |WHERE {
        |  ?suppl schema:manufacturer <${product.uri}> .
        |  ?suppl schema:legalName ?name .
        |  ?suppl schema:location ?loc .
        |  ?loc schema:address ?adr .
        |  ?loc geo:lat ?lat .
        |  ?loc geo:long ?long .
        |  OPTIONAL { ?adr schema:streetAddress ?street . }
        |  OPTIONAL { ?adr schema:postalCode ?zip . }
        |  OPTIONAL { ?adr schema:addressLocality ?city . }
        |  OPTIONAL { ?adr schema:addressCountry ?country . }
        |  OPTIONAL {
        |    ?loc ogcgs:asWKT ?wktPoint .
        |    ?geometry ogcgs:asWKT ?wkt .
        |    FILTER(bif:st_within(?wktPoint, ?wkt)) .
        |    ?feature <http://geovocab.org/geometry#geometry> ?geometry .
        |    ?level <http://linkedgeodata.org/ld/gadm2/ontology/representedBy> ?feature .
        |    ?level <http://linkedgeodata.org/ld/gadm2/ontology/level> ?levelNr .
        |    FILTER(?levelNr > 2) .
        |  }
        |} LIMIT 1
      """.stripMargin
    val prefixedQS = prefix(queryStr)
    //log.info(prefixedQS)
    val result = epc.getEndpoint().select(prefixedQS).toSeq

    var s: Supplier = null
    for (binding <- result) {
      val uri = binding.getResource("suppl").getURI
      val name = binding.getLiteral("name").getString
      var street = ""
      if (binding.getLiteral("street") != null)
        street = binding.getLiteral("street").getString
      var zip = ""
      if (binding.getLiteral("zip") != null)
        zip = binding.getLiteral("zip").getString
      var city = ""
      if (binding.getLiteral("city") != null)
        city = binding.getLiteral("city").getString
      var country = ""
      if (binding.getLiteral("country") != null)
        country = binding.getLiteral("country").getString
      val long = binding.getLiteral("long").getFloat
      val lat = binding.getLiteral("lat").getFloat
      var feature = ""
      if (binding.getResource("feature") != null)
        feature = binding.getResource("feature").getURI
      val adr = new Address(street, zip, city, country)
      val coords = new Coordinates(lat, long)
      val ws = wp.getNearesWeaterStation(coords)
      s = new Supplier(uri, name, adr, coords, product, ws, feature)
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


