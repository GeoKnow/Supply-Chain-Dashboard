package controllers

import play.api.mvc._
import models.CurrentDataset
import play.api.libs.iteratee.Concurrent
import play.api.libs.Comet
import play.api.libs.Comet.CometMessage
import scala.util.Random
import supplychain.model.{Order, Message, Shipping, Connection}

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

  def metrics = Action {
    Ok(views.html.metrics())
  }

  def deliveryStream = Action {
    val (orderEnumerator, orderChannel) = Concurrent.broadcast[Order]
    val (shippingEnumerator, shippingChannel) = Concurrent.broadcast[Shipping]

    // Listen for new orders and shippings
    val listener = (msg: Message) => msg match {
      case order: Order => orderChannel.push(order)
      case shipping: Shipping => shippingChannel.push(shipping)
    }
    CurrentDataset().addListener(listener)

    /** Computes the number of due parts (i.e., parts that have been ordered but not delivered yet) */
    def dueParts(connection: Connection) = {
      var due = 0
      for(msg <- CurrentDataset().messages.filter(_.connection.id == connection.id)) msg match {
        case o: Order => due += o.count
        case s: Shipping => due -= s.count
      }
      due
    }

    implicit val orderMessage = CometMessage[Order](d => s"'${d.connection.receiver.id}', '${d.connection.id}', ${dueParts(d.connection)}")
    implicit val shippingMessage = CometMessage[Shipping](d => s"'${d.connection.receiver.id}', '${d.connection.id}', ${dueParts(d.connection)}")

    val orderEnumeratee = orderEnumerator &> Comet(callback = "parent.addOrder")
    val shippingEnumeratee = shippingEnumerator &> Comet(callback = "parent.addShipping")

    Ok.chunked(orderEnumeratee interleave shippingEnumeratee)
  }

}