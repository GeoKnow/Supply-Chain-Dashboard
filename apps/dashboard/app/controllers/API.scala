package controllers

import play.api.mvc.{Action, Controller}
import com.hp.hpl.jena.query.ResultSetFormatter
import models._
import play.api.Logger
import scala.Some
import java.io.StringWriter
import supplychain.dataset.{SchnelleckeDataset, Namespaces}

/**
 * The REST API.
 */
object API extends Controller {

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
   *
   * @param id
   * @param format One of: "HTML", "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE", "TURTLE", "TTL"
   * @return
   */
  def supplier(id: String, format: Option[String]) = Action { implicit request =>
    val address = CurrentDataset().suppliers.find(_.id == id).get

    render {
      case _ if format.exists(_.toLowerCase == "html") => {
        Ok(views.html.supplierView(address))
      }
      case Accepts.Html() if format.isEmpty => {
        Ok(views.html.supplierView(address))
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
   *
   * @param id
   * @param format One of: "HTML", "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE", "TURTLE", "TTL"
   * @return
   */
  def delivery(id: String, format: Option[String]) = Action { implicit request =>
    val delivery = CurrentDataset().connections.find(_.id == id).get

    render {
      case _ if format.exists(_.toLowerCase == "html") => {
        Ok(views.html.deliveryView(delivery))
      }
      case Accepts.Html() if format.isEmpty => {
        Ok(views.html.deliveryView(delivery))
      }
      case _ => {
        val model = CurrentDataset().describe(s"DESCRIBE <${Namespaces.delivery + id}>")
        val writer = new StringWriter()
        model.write(writer, format.getOrElse("TURTLE"))
        Ok(writer.toString).as("text/turtle")
      }
    }
  }

  def suppliers() = Action {
    val suppliers = CurrentDataset().suppliers
    Ok(views.html.suppliers(suppliers))
  }

  def deliveries(addressId: Option[String], contentType: Option[String]) = Action {
    // Retrieve deliveries
    val deliveries = addressId match {
      // Address provided => Only return deliveries that depart or arrive at the specified address
      case Some(id) => CurrentDataset().connections.filter(d => d.sender.id == id || d.receiver.id == id)
      // No address provided => Check if contentType is provided
      case None => contentType match {
        case Some(content) => CurrentDataset().connections.filter(_.content == content)
        case None => CurrentDataset().connections
      }
    }

    Ok(views.html.deliveries(deliveries))
  }

  def loadSchnelleckeDataset() = Action {
    CurrentDataset() = new SchnelleckeDataset()
    Ok
  }

  def loadSourceMapDataset(id: Int) = Action {
    CurrentDataset() = new SourceMapDataset(id)
    Ok
  }

}
