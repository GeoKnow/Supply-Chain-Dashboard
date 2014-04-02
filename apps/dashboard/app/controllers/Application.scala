package controllers

import play.api.mvc._
import models.CurrentDataset
import play.api.libs.iteratee.Concurrent
import play.api.libs.Comet
import play.api.libs.Comet.CometMessage
import scala.util.Random
import dataset.{Shipping, Connection}

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

  def supplier(id: String) = Action {
    val address = CurrentDataset().suppliers.find(_.id == id).get
    Ok(views.html.supplierView(address))
  }

  def delivery(id: String) = Action {
    val delivery = CurrentDataset().deliveries.find(_.id == id).get
    Ok(views.html.deliveryView(delivery))
  }

  def deliveryStream = Action {
    val (enumerator, channel) = Concurrent.broadcast[Shipping]

    val listener = (delivery: Shipping) => channel.push(delivery)

    CurrentDataset().addListener(listener)

    //implicit val deliveryMessage = CometMessage[Shipping](d => s"'${d.id}', ${d.sender.coords.lat},${d.sender.coords.lon},${d.receiver.coords.lat},${d.receiver.coords.lon}")

    //Ok.chunked(enumerator &> Comet(callback = "parent.addShipping"))
    Ok
  }

}