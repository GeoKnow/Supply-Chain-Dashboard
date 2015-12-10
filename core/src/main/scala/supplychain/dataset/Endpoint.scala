package supplychain.dataset

import java.io.{File, OutputStreamWriter}
import java.net.{HttpURLConnection, URL, URLEncoder}
import java.util.logging.Logger
import com.eccenca.elds.virtuoso.SparqlEndpoint
import com.hp.hpl.jena.query.{DatasetFactory, QueryExecutionFactory, QueryFactory, ResultSet}
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.update.UpdateAction
import org.apache.jena.riot.Lang
import play.api.libs.json.{Json, Writes}

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
  def uploadDataset(graph: String, file: File, lang: Option[Lang]=None, clear: Boolean=false)
  def describe(query: String): Model
  def createGraph(graphUri: String, clear: Boolean): Unit
}

case class EndpointConfig(doInit: Boolean,
                          kind: String,
                          var defaultGraph: String,
                          defaultGraphWeather: String = null,
                          defaultGraphConfiguration: String = null,
                          url: String = "",
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
      endpoint = new RemoteEndpoint(url, defaultGraph)
    }
    if (kind == TYPE_JDBC_VIRTUOSO) {
      endpoint = new VirtuosoJdbcEndpoint(host, port, user, password)
    }

    if (endpoint != null) {
      if (doInit) initData()
      return endpoint
    } else
      throw new Exception("Unable to create Endpoint.")
  }

  def initData() {
    if (!isDataInitialized) {
      endpoint.createGraph(getDefaultGraph(), false)

      val weatherStationFile = new File("data/conf/ncdc-stations.ttl.gz")
      endpoint.uploadDataset(getDefaultGraphWeather(), weatherStationFile, Option(Lang.TTL))
      val weatherFile = new File("data/conf/ncdc-ghcnd-obs.ttl.gz")
      endpoint.uploadDataset(getDefaultGraphWeather(), weatherFile, Option(Lang.TTL))

      val supplConfFile = new File("data/conf/supplier.ttl")
      endpoint.uploadDataset(getDefaultGraphConfiguration(), supplConfFile, Option(Lang.TTL), true)
      val supplAddConfFile = new File("data/conf/supplier_add.ttl")
      endpoint.uploadDataset(getDefaultGraphConfiguration(), supplAddConfFile, Option(Lang.TTL))
      val prodConfFile = new File("data/conf/products.ttl")
      endpoint.uploadDataset(getDefaultGraphConfiguration(), prodConfFile, Option(Lang.TTL))
      val prodAddConfFile = new File("data/conf/products_add.ttl")
      endpoint.uploadDataset(getDefaultGraphConfiguration(), prodAddConfFile, Option(Lang.TTL))
    }
    isDataInitialized = true
  }

  def defaultGraphMetrics(): String = { defaultGraph + "metrics/" }

  def getDefaultGraph(): String = defaultGraph
  def getDefaultGraphWeather(): String = defaultGraphWeather
  def getDefaultGraphMetrics(): String = defaultGraphMetrics
  def getDefaultGraphConfiguration(): String = defaultGraphConfiguration
}

object EndpointConfig {
  implicit val endpointConfigWrites = new Writes[EndpointConfig] {
    def writes(ec: EndpointConfig) = Json.obj(
      "doInit" -> ec.doInit,
      "kind" -> ec.kind,
      "defaultGraph" -> ec.defaultGraph,
      "defaultGraphWeather" -> ec.defaultGraphWeather,
      "defaultGraphConfiguration" -> ec.defaultGraphConfiguration,
      "url" -> ec.url,
      "host" -> ec.host,
      "port" -> ec.port,
      "user" -> ec.user,
      "password" -> ec.password
    )
  }
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

  override def uploadDataset(graph: String, file: File, lang: Option[Lang], clear: Boolean=false): Unit = ???

  override def createGraph(graphUri: String, clear: Boolean): Unit = {
    if (clear) update(s"DROP SILENT GRAPH <${graphUri}>")
    update(s"CREATE SILENT GRAPH <${graphUri}>")
  }
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

  override def uploadDataset(graph: String, file: File, lang: Option[Lang], clear: Boolean=false): Unit = {
    endpoint.uploadDataset(graph, file, lang, clear)
  }

  override def createGraph(graphUri: String, clear: Boolean): Unit = {
    endpoint.createGraph(graphUri, clear)
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

  override def uploadDataset(graph: String, file: File, lang: Option[Lang], clear: Boolean=false): Unit = ???

  override def createGraph(graphUri: String, clear: Boolean): Unit = {
    if (clear) update(s"DROP SILENT GRAPH <${graphUri}>")
    update(s"CREATE SILENT GRAPH <${graphUri}>")
  }
}