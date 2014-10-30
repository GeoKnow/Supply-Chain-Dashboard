package supplychain.dataset

import java.io.OutputStreamWriter
import java.net.{HttpURLConnection, URL, URLEncoder}
import java.util.logging.Logger
import com.hp.hpl.jena.query.{DatasetFactory, QueryExecutionFactory, QueryFactory, ResultSet}
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.update.UpdateAction

import scala.io.Source

/**
 * An RDF endpoint, which may either be a local or a remote endpoint.
 */
trait Endpoint {
  def update(query: String)
  def select(query: String): ResultSet
  def describe(query: String): Model
}

class LocalEndpoint(defaultGraph: String) extends Endpoint {

  private val dataset = DatasetFactory.createMem()

  def update(query: String) = {
    UpdateAction.parseExecute(query, dataset.getNamedModel(defaultGraph))
  }

  def select(query: String) = {
    val parsedQuery = QueryFactory.create(query)
    QueryExecutionFactory.create(parsedQuery, dataset.getNamedModel(defaultGraph)).execSelect()
  }

  def describe(query: String) = {
    val parsedQuery = QueryFactory.create(query)
    QueryExecutionFactory.create(parsedQuery, dataset.getNamedModel(defaultGraph)).execDescribe()
  }
}

class RemoteEndpoint(endpointUrl: String, defaultGraph: String) extends Endpoint {

  private val log = Logger.getLogger(classOf[RemoteEndpoint].getName)

  def update(query: String) = {
    //Open a new HTTP connection
    val url = new URL(endpointUrl)
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("POST")
    connection.setDoOutput(true)
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

    // Write query
    val writer = new OutputStreamWriter(connection.getOutputStream, "UTF-8")
    try {
      writer.write("default-graph-uri=" + URLEncoder.encode(defaultGraph, "UTF-8"))
      writer.write("&query=")
      writer.write(URLEncoder.encode(query, "UTF-8"))
    } finally {
      writer.close()
    }

    //Check if the HTTP response code is in the range 2xx
    if (connection.getResponseCode / 100 == 2) {
      log.fine("Update query send:\n" + query)
    }
    else {
      val errorStream = connection.getErrorStream
      if (errorStream != null) {
        val errorMessage = Source.fromInputStream(errorStream).getLines.mkString("\n")
        log.warning("SPARQL/Update query on " + endpointUrl + " failed. Error Message: '" + errorMessage + "'.")
      }
      else {
        log.warning("SPARQL/Update query on " + endpointUrl + " failed. Server response: " + connection.getResponseCode + " " + connection.getResponseMessage + ".")
      }
    }
  }

  def select(query: String) = {
    val parsedQuery = QueryFactory.create(query)
    QueryExecutionFactory.sparqlService(endpointUrl, parsedQuery).execSelect()
  }

  def describe(query: String) = {
    val parsedQuery = QueryFactory.create(query)
    QueryExecutionFactory.sparqlService(endpointUrl, parsedQuery).execDescribe()
  }
}