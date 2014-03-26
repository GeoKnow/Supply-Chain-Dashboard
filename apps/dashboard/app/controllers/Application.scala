package controllers

import play.api.mvc._
import models.CurrentDataset
import play.api.libs.iteratee.Concurrent
import simulation.Shipping
import play.api.libs.Comet
import play.api.libs.Comet.CometMessage
import scala.util.Random
import dataset.Delivery

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
    val address = CurrentDataset().addresses.find(_.id == id).get
    Ok(views.html.addressView(address))
  }

  def delivery(id: String) = Action {
    val delivery = CurrentDataset().deliveries.find(_.id == id).get
    Ok(views.html.deliveryView(delivery))
  }

  def deliveryStream = Action {
    val (enumerator, channel) = Concurrent.broadcast[Delivery]

    val listener = (delivery: Delivery) => channel.push(delivery)

    CurrentDataset().addListener(listener)

    implicit val deliveryMessage = CometMessage[Delivery](d => s"'${d.id}', ${d.sender.latitude},${d.sender.longitude},${d.receiver.latitude},${d.receiver.longitude}")

    Ok.chunked(enumerator &> Comet(callback = "parent.addDelivery"))
  }

}