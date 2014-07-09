package supplychain.dataset

import java.io.OutputStreamWriter
import java.net.{HttpURLConnection, URL, URLEncoder}

import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryFactory, ResultSet}
import com.hp.hpl.jena.rdf.model.{Model, ModelFactory}
import com.hp.hpl.jena.update.UpdateAction

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

  def update(query: String) = {
    //Open a new HTTP connection
    val url = new URL(endpointUrl)
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("POST")
    connection.setDoOutput(true)
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

    // Write query
    val writer = new OutputStreamWriter(connection.getOutputStream, "UTF-8")
    writer.write("query=")
    writer.write(URLEncoder.encode(query, "UTF-8"))
    writer.close()

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