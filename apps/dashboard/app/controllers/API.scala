package controllers

import play.api.mvc.{Action, Controller}
import com.hp.hpl.jena.query.ResultSetFormatter
import models._
import play.api.Logger
import scala.Some

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

  def suppliers() = Action {
    val suppliers = CurrentDataset().suppliers
    Ok(views.html.suppliers(suppliers))
  }

  def deliveries(addressId: Option[String], contentType: Option[String]) = Action {
    // Retrieve deliveries
    val deliveries = addressId match {
      // Address provided => Only return deliveries that depart or arrive at the specified address
      case Some(id) => CurrentDataset().deliveries.filter(d => d.sender.id == id || d.receiver.id == id)
      // No address provided => Check if contentType is provided
      case None => contentType match {
        case Some(content) => CurrentDataset().deliveries.filter(_.content == content)
        case None => CurrentDataset().deliveries
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
