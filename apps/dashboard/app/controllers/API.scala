package controllers

import play.api.mvc.{Action, Controller}
import com.hp.hpl.jena.query.ResultSetFormatter
import models._
import play.api.Logger
import scala.Some
import java.io.StringWriter
import supplychain.dataset.{DatasetStatistics, SchnelleckeDataset, Namespaces}
import supplychain.simulator.{Simulator, Simulation}
import supplychain.metric.Metrics

/**
 * The REST API.
 */
object API extends Controller {

  /**
   * Issues a SPARQL select query.
   * @param query The query
   * @return A html page, if the ACCEPT header includes html.
   *         Otherwise, SPARQL results XML.
   */
  def sparql(query: String) = Action { implicit request =>
    Logger.info("Received query:\n" + query)
    val resultSet = CurrentDataset().query(query)
    val resultXML = ResultSetFormatter.asXMLString(resultSet)

    render {
      case Accepts.Html() => {
        val xml = scala.xml.XML.loadString(resultXML)
        Ok(views.html.queryResults(xml))
      }
      case _ => {
        Ok(resultXML).as("application/sparql-results+xml")
      }
    }
  }

  /**
   * Retrieves a supplier.
   *
   * @param id The id of the supplier
   * @param format One of: "HTML", "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE", "TURTLE", "TTL"
   * @return
   */
  def supplier(id: String, format: Option[String]) = Action { implicit request =>
    val supplier = CurrentDataset().suppliers.find(_.id == id).get
    val incomingConnections = CurrentDataset().connections.filter(_.target.id == id)
    val outgoingConnections = CurrentDataset().connections.filter(_.source.id == id)

    render {
      case _ if format.exists(_.toLowerCase == "html") => {
        Ok(views.html.supplierDetails(supplier, incomingConnections, outgoingConnections))
      }
      case Accepts.Html() if format.isEmpty => {
        Ok(views.html.supplierDetails(supplier, incomingConnections, outgoingConnections))
      }
      case _ => {
        val model = CurrentDataset().describe(s"DESCRIBE <${Namespaces.supplier + id}>")
        val writer = new StringWriter()
        model.write(writer, format.getOrElse("TURTLE"))
        Ok(writer.toString).as("text/turtle")
      }
    }
  }

  /**
   * Retrieves a connection.
   *
   * @param id The id of the connection
   * @param format One of: "HTML", "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE", "TURTLE", "TTL"
   * @return
   */
  def connection(id: String, format: Option[String]) = Action { implicit request =>
    val connection = CurrentDataset().connections.find(_.id == id).get

    render {
      case _ if format.exists(_.toLowerCase == "html") => {
        Ok(views.html.connectionDetails(connection))
      }
      case Accepts.Html() if format.isEmpty => {
        Ok(views.html.connectionDetails(connection))
      }
      case _ => {
        val model = CurrentDataset().describe(s"DESCRIBE <${Namespaces.connection + id}>")
        val writer = new StringWriter()
        model.write(writer, format.getOrElse("TURTLE"))
        Ok(writer.toString).as("text/turtle")
      }
    }
  }

  def loadSuppliers() = Action {
    val suppliers = CurrentDataset().suppliers
    val stats = new DatasetStatistics(CurrentDataset())
    val dueOrders = suppliers.map(stats.dueParts)

    Ok(views.html.loadSuppliers(suppliers, dueOrders))
  }

  def loadConnections(addressId: Option[String], contentType: Option[String]) = Action {
    // Retrieve connections
    val connections = addressId match {
      // Address provided => Only return deliveries that depart or arrive at the specified address
      case Some(id) => CurrentDataset().connections.filter(d => d.source.id == id || d.target.id == id)
      // No address provided => Check if contentType is provided
      case None => contentType match {
        case Some(content) => CurrentDataset().connections.filter(_.content.name == content)
        case None => CurrentDataset().connections
      }
    }

    Ok(views.html.loadConnections(connections))
  }

  def loadSchnelleckeDataset() = Action {
    CurrentDataset() = new SchnelleckeDataset()
    Ok
  }

  def loadSourceMapDataset(id: Int) = Action {
    CurrentDataset() = new SourceMapDataset(id)
    Ok
  }

  def step() = Action {
    CurrentDataset.simulator.step()
    Ok
  }

  def run(frequency: Double) = Action {
    CurrentDataset.simulator.run(frequency)
    Ok
  }

  def stop() = Action {
    CurrentDataset.simulator.stop()
    Ok
  }

  def reloadMetrics() = Action {
    CurrentMetrics.load()
    Ok
  }
}
