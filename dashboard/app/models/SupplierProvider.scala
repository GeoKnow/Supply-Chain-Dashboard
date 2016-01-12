package models

import java.util.logging.Logger

import com.hp.hpl.jena.query.QuerySolution
import supplychain.dataset.EndpointConfig

import scala.collection.JavaConversions._

/**
 * Created by rene on 29.06.15.
 */
class SupplierProvider(ec: EndpointConfig) {

  private val log = Logger.getLogger(getClass.getName)

  def getConfiguredSupplierUris(): Map[String, String] = {
    var searchStrings: Map[String, String] = Map()
    val query =
      s"""
         |SELECT DISTINCT ?supplier FROM <${Configuration.get.endpointConfig.defaultGraph}>
         |WHERE {
         |  ?conn a sc:Connection .
         |  ?conn sc:sender|sc:receiver ?supplier .
         |}
       """.stripMargin
    val res = select(query)
    for (r <- res) {
      val uri = r.getResource("supplier").toString()
      searchStrings += getLabelQueryString(uri)
      searchStrings += getLocationQueryString(uri)
    }

    log.info("search terms: ")
    for((k,v) <- searchStrings) {
      log.info(s"${k} - ${v}")
    }
    searchStrings
  }

  def getLabelQueryString(supplierUri: String): (String, String) = {
    var queryString: String = null
    val query =
      s"""
         |SELECT DISTINCT ?label FROM <${Configuration.get.endpointConfig.defaultGraphConfiguration}>
         |WHERE {
         |  <${supplierUri}> schema:name|schema:legalName|schema:alternateName ?label .
         |}
       """.stripMargin
    val res = select(query)
    for (r <- res) {
      val label = r.getLiteral("label").toString()
      if (queryString != null && queryString.length > 0)
        queryString += " OR \"" + label + "\""
      else
        queryString = "\"" + label + "\""
    }
    (supplierUri, queryString)
  }

  def getLocationQueryString(supplierUri: String): (String, String) = {
    var loc: String = null
    var adr: String = null
    val query =
      s"""
         |SELECT DISTINCT ?adr ?city FROM <${Configuration.get.endpointConfig.defaultGraphConfiguration}>
         |WHERE {
         |  <${supplierUri}> schema:location ?loc .
         |  ?loc schema:address ?adr .
         |  ?adr schema:addressLocality ?city .
         |} LIMIT 1
       """.stripMargin
    val res = select(query)
    for (r <- res) {
      adr = r.getResource("adr").toString
      loc = "\"" + r.getLiteral("city").toString() + "\""
    }
    (adr, loc)
  }

  def select(query: String): Seq[QuerySolution] = {
    val q = s"""
               | PREFIX sno: <http://www.xybermotive.com/newsOnt/>
               | PREFIX sni: <http://www.xybermotive.com/news/>
               | PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
               | PREFIX schema: <http://schema.org/>
               | PREFIX sc: <http://www.xybermotive.com/ontology/>
               | PREFIX suppl: <http://www.xybermotive.com/supplier/>
               | $query
      """.stripMargin
    ec.getEndpoint().select(q).toList
  }


}
