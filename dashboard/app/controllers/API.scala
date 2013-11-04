package controllers

import play.api.mvc.{Action, Controller}
import com.hp.hpl.jena.query.ResultSetFormatter
import models.Dataset
import play.api.Logger

/**
 * The REST API.
 */
object API extends Controller {

  def sparql(query: String) = Action { implicit request =>
    Logger.info("Received query:\n" + query)
    val resultSet = Dataset.query(query)
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

  def deliveries(sourceId: String) = Action {
    val deliveries = Dataset.deliveries.filter(d => d.sender.id == sourceId || d.receiver.id == sourceId)

    Ok(views.html.showDeliveries(deliveries))
  }

}
