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
    val addresses = Dataset.addresses

    Ok(views.html.map(addresses))
  }

  def delivery(id: String) = Action {
    val delivery = Dataset.retrieveDelivery(id)

    Ok(views.html.deliveryView(delivery))
  }

}