package supplychain.simulator

import java.util.logging.Logger

import play.api.libs.ws
import supplychain.dataset.EndpointConfig
import supplychain.model.{Coordinates, DateTime, Duration, WeatherObservation, WeatherStation, Address, Supplier}
import supplychain.model.Product

import scala.collection.JavaConversions._

/**
 * Created by rene on 09.01.15.
 */
class ConfigurationProvider(ec: EndpointConfig, wp: WeatherProvider_, productUri: String) {

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

  /**
   * returns the product structure for the given product uri or the configured simulation product if omitted
   */
  def getProduct(uri: String = productUri): Product = {

    val queryStr =
      s"""
        |SELECT DISTINCT ?name FROM <${ec.getDefaultGraphConfiguration()}>
        |WHERE {
        |  <${uri}> schema:name ?name .
        |  #FILTER( LANGMATCHES( LANG( ?name ), "DE" ) ) .
        |} LIMIT 1
      """.stripMargin

    val prefixedQS = prefix(queryStr)
    //log.info(prefixedQS)
    val result = ec.getEndpoint().select(prefixedQS).toSeq

    var p: Product = null
    for (binding <- result) {
      val name = binding.getLiteral("name").getString
      p = new Product(name, 1, getParts(uri), uri)
    }
    log.info("# PRODUCT: " + p.toString)
    p
  }

  def getSupplier(product: Product): Supplier = {
    val queryStr =
      s"""
        |PREFIX suppl: <http://www.xybermotive.com/supplier/>
        |PREFIX schema: <http://schema.org/>
        |PREFIX prod: <http://www.xybermotive.com/products/>
        |
        |SELECT DISTINCT ?suppl ?name ?street ?zip ?city ?country ?long ?lat FROM <${ec.getDefaultGraphConfiguration()}>
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
    val result = ec.getEndpoint().select(prefixedQS).toSeq

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

  private def getParts(uri: String): List[Product] = {
    val queryStr =
      s"""
        |SELECT ?uri ?name ?quantity FROM <${ec.getDefaultGraphConfiguration()}>
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
    val result = ec.getEndpoint().select(prefixedQS).toSeq

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
