package controllers

import play.api.mvc.{Action, Controller}
import com.hp.hpl.jena.query.ResultSetFormatter
import models.{SourceMapDataset, SchnelleckeDataset, Dataset}
import play.api.Logger

/**
 * The REST API.
 */
object API extends Controller {

  def sparql(query: String) = Action { implicit request =>
    Logger.info("Received query:\n" + query)
    val resultSet = Dataset().query(query)
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

  def addresses() = Action {
    val addresses = Dataset().addresses
    Ok(views.html.addresses(addresses))
  }

  def deliveries(addressId: Option[String], contentType: Option[String]) = Action {
    // Retrieve deliveries
    val deliveries = addressId match {
      // Address provided => Only return deliveries that depart or arrive at the specified address
      case Some(id) => Dataset().deliveries.filter(d => d.sender.id == id || d.receiver.id == id)
      // No address provided => Check if contentType is provided
      case None => contentType match {
        case Some(content) => Dataset().deliveries.filter(_.content == content)
        case None => Dataset().deliveries
      }
    }

    Ok(views.html.deliveries(deliveries))
  }

  def loadSchnelleckeDataset() = Action {
    Dataset() = new SchnelleckeDataset()
    Ok
  }

  def loadSourceMapDataset(id: Int) = Action {
    Dataset() = new SourceMapDataset(id)
    Ok
  }

}