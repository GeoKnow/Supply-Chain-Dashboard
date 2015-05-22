package supplychain.simulator

import java.util.logging.Logger

import play.api.libs.ws
import supplychain.dataset.EndpointConfig
import supplychain.model.{Coordinates, DateTime, Duration, WeatherObservation, WeatherStation, WeatherUtil}
import supplychain.model.Product

import scala.collection.JavaConversions._

/**
 * Created by rene on 09.01.15.
 */
class ConfigurationProvider(ec: EndpointConfig, productUri: String) {

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
        |SELECT ?name FROM <${ec.getDefaultGraphConfiguration()}>
        |WHERE {
        |  <${uri}> schema:name ?name .
        |  FILTER( LANGMATCHES( LANG( ?name ), "DE" ) ) .
        |}
      """.stripMargin

    val prefixedQS = prefix(queryStr)
    //log.info(prefixedQS)
    val result = ec.getEndpoint().select(prefixedQS).toSeq

    var p: Product = null
    for (binding <- result) {
      val name = binding.getLiteral("name").getString
      p = new Product(name, 1, getParts(uri), uri)
    }
    p
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
        |  FILTER( LANGMATCHES( LANG( ?name ), "DE" ) ) .
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
