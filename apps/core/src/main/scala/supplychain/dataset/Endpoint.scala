package supplychain.dataset

import java.io.OutputStreamWriter
import java.net.{HttpURLConnection, URL, URLEncoder}
import java.util.logging.Logger

import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryFactory, ResultSet}
import com.hp.hpl.jena.rdf.model.{Model, ModelFactory}
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

class LocalEndpoint extends Endpoint {
  // Create new model
  private val model = ModelFactory.createDefaultModel()

  def update(query: String) = {
    UpdateAction.parseExecute(query, model)
  }

  def select(query: String) = {
    val parsedQuery = QueryFactory.create(query)
    QueryExecutionFactory.create(parsedQuery, model).execSelect()
  }

  def describe(query: String) = {
    val parsedQuery = QueryFactory.create(query)
    QueryExecutionFactory.create(parsedQuery, model).execDescribe()
  }
}

class RemoteEndpoint(endpointUrl: String) extends Endpoint {

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
      writer.write("query=")
      writer.write(URLEncoder.encode(query, "UTF-8"))
    } finally {
      writer.close()
    }

    //Check if the HTTP response code is in the range 2xx
    if (connection.getResponseCode / 100 == 2) {
      log.info("Update query send:\n" + query)
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

    //val parsedQuery = UpdateFactory.create(query)
    //UpdateExecutionFactory.createRemote(parsedQuery, endpointUrl).execute()
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