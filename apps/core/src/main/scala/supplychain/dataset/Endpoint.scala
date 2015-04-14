package supplychain.dataset

import java.io.{File, OutputStreamWriter}
import java.net.{HttpURLConnection, URL, URLEncoder}
import java.util.logging.Logger
import com.eccenca.elds.virtuoso.SparqlEndpoint
import com.hp.hpl.jena.query.{DatasetFactory, QueryExecutionFactory, QueryFactory, ResultSet}
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.update.UpdateAction
import org.apache.jena.riot.Lang

import scala.io.Source

/**
 * An RDF endpoint, which may either be a local or a remote endpoint.
 */
trait Endpoint {
  var TYPE_LOCAL = "local"
  var TYPE_JDBC = "jdbc"
  var TYPE_HTTP = "http"

  def update(query: String)
  def select(query: String): ResultSet
  def uploadDataset(graph: String, file: File, lang: Option[Lang]=None)
  def describe(query: String): Model
}

class EndpointConfig(kind: String,
                     defaultGraph: String,
                     defaultGraphWeather: String = null,
                     http: String = "",
                     host: String = "",
                     port: String = "",
                     user: String = "",
                     password: String = "") {

  var TYPE_LOCAL = "local"
  var TYPE_HTTP_SPARQL = "sparql"
  var TYPE_JDBC_VIRTUOSO = "virtuoso"

  private var endpoint: Endpoint = null
  private var isDataInitialized = false

  def getEndpoint(): Endpoint = {
    if (endpoint != null) return endpoint

    if (kind == TYPE_LOCAL) {
      endpoint = new LocalEndpoint(defaultGraph)
    }
    if (kind == TYPE_HTTP_SPARQL) {
      endpoint = new RemoteEndpoint(http, defaultGraph)
    }
    if (kind == TYPE_JDBC_VIRTUOSO) {
      endpoint = new VirtuosoJdbcEndpoint(host, port, user, password)
    }

    if (endpoint != null) {
      initData()
      return endpoint
    } else
      return null
  }

  def initData() {
    if (!isDataInitialized) {
      endpoint.update(s"CREATE SILENT GRAPH <${getDefaultGraph()}>")
      endpoint.update(s"CREATE SILENT GRAPH <${getDefaultGraphWeather()}>")
      val file = new File("dashboard/data/ncdc-ghcnd_2010-2013.nt.gz")
      endpoint.uploadDataset(getDefaultGraphWeather(), file, Option(Lang.NT))
    }
    isDataInitialized = true
  }

  def getDefaultGraph(): String = defaultGraph
  def getDefaultGraphWeather(): String = defaultGraphWeather

}

class LocalEndpoint(defaultGraph: String) extends Endpoint {

  private val dataset = DatasetFactory.createMem()

  override def update(query: String) = {
    UpdateAction.parseExecute(query, dataset.getNamedModel(defaultGraph))
  }

  override def select(query: String) = {
    val parsedQuery = QueryFactory.create(query)
    QueryExecutionFactory.create(parsedQuery, dataset.getNamedModel(defaultGraph)).execSelect()
  }

  override def describe(query: String) = {
    val parsedQuery = QueryFactory.create(query)
    QueryExecutionFactory.create(parsedQuery, dataset.getNamedModel(defaultGraph)).execDescribe()
  }

  override def uploadDataset(graph: String, file: File, lang: Option[Lang]): Unit = ???
}

class VirtuosoJdbcEndpoint(host: String, port: String, user: String, password: String) extends Endpoint {

  val endpoint = new SparqlEndpoint(host, port, user, password)

  override def update(query: String): Unit = {
    endpoint.update(query)
  }

  override def select(query: String): ResultSet = {
    endpoint.select(query)
  }

  override def describe(query: String): Model = {
    endpoint.describe(query)
  }

  override def uploadDataset(graph: String, file: File, lang: Option[Lang]): Unit = {
    endpoint.uploadDataset(graph, file, lang)
  }
}

class RemoteEndpoint(endpointUrl: String, defaultGraph: String) extends Endpoint {

  private val log = Logger.getLogger(classOf[RemoteEndpoint].getName)

  override def update(query: String) = {
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

  override def select(query: String) = {
    val parsedQuery = QueryFactory.create(query)
    QueryExecutionFactory.sparqlService(endpointUrl, parsedQuery).execSelect()
  }


  override def describe(query: String) = {
    val parsedQuery = QueryFactory.create(query)
    QueryExecutionFactory.sparqlService(endpointUrl, parsedQuery).execDescribe()
  }

  override def uploadDataset(graph: String, file: File, lang: Option[Lang]): Unit = ???
}