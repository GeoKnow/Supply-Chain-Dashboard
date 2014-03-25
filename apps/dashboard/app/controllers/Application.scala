package controllers

import play.api._
import play.api.mvc._
import models.Dataset
import play.api.libs.iteratee.{Concurrent, Enumerator}
import simulation.{Shipping, Simulation}
import play.api.libs.Comet
import play.api.libs.Comet.CometMessage

object Application extends Controller {

  def print() {
  }

  def index = Action {
    print()
    Ok(views.html.index())
  }

  def query = Action {
    Ok(views.html.query())
  }

  def map = Action {
    Ok(views.html.map())
  }

  def address(id: String) = Action {
    val address = Dataset().addresses.find(_.id == id).get
    Ok(views.html.addressView(address))
  }

  def delivery(id: String) = Action {
    val delivery = Dataset().deliveries.find(_.id == id).get
    Ok(views.html.deliveryView(delivery))
  }

  def deliveryStream = Action {
    val (enumerator, channel) = Concurrent.broadcast[Shipping]

    val listener = (shipping: Shipping) => channel.push(shipping)

    Simulation.addListener(listener)

    implicit val shippingMessage = CometMessage[Shipping](s => s"${s.sender.lat},${s.sender.lon},${s.receiver.lat},${s.receiver.lon}")

    Ok.chunked(enumerator &> Comet(callback = "addDelivery"))
  }

}