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
    Ok(views.html.map())
  }

  def address(id: String) = Action {
    val address = Dataset.addresses.find(_.id == id).get
    Ok(views.html.addressView(address))
  }

  def delivery(id: String) = Action {
    val delivery = Dataset.deliveries.find(_.id == id).get
    Ok(views.html.deliveryView(delivery))
  }

}