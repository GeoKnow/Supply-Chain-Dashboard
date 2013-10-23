package controllers

import play.api.mvc.{Action, Controller}
import com.hp.hpl.jena.query.ResultSetFormatter
import models.Dataset
import play.api.Logger

/**
 * The REST API.
 */
object API extends Controller {

  def sparql(query: String) = Action {
    Logger.info("Received query:\n" + query)
    val resultSet = Dataset.query(query)
    val resultXML = ResultSetFormatter.asXMLString(resultSet)

    Ok(resultXML).as("application/sparql-results+xml")
  }

}
