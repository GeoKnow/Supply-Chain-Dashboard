package controllers

import play.api.mvc._
import models.CurrentDataset
import play.api.libs.iteratee.Concurrent
import play.api.libs.Comet
import play.api.libs.Comet.CometMessage
import scala.util.Random
import dataset.{Order, Message, Shipping, Connection}

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

  def deliveryStream = Action {
    val (orderEnumerator, orderChannel) = Concurrent.broadcast[Order]
    val (shippingEnumerator, shippingChannel) = Concurrent.broadcast[Shipping]

    val listener = (msg: Message) => msg match {
      case order: Order => orderChannel.push(order)
      case shipping: Shipping => shippingChannel.push(shipping)
    }

    CurrentDataset().addListener(listener)

    implicit val orderMessage = CometMessage[Order](d => s"'${d.connection.id}'")
    implicit val shippingMessage = CometMessage[Shipping](d => s"'${d.connection.id}'")

    val orderEnumeratee = orderEnumerator &> Comet(callback = "parent.addOrder")
    val shippingEnumeratee = shippingEnumerator &> Comet(callback = "parent.addShipping")

    Ok.chunked(orderEnumeratee interleave shippingEnumeratee)
  }

}