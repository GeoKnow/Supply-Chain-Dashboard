package controllers

import play.api._
import play.api.mvc._
import models.Dataset
import scala.collection.JavaConversions._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def query = Action {
    Ok(views.html.query())
  }

  def map = Action {
    val deliveries = Dataset.queryDeliveries()

    Ok(views.html.map(deliveries))
  }

}