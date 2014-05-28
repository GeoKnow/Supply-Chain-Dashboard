package controllers

import play.api.mvc._
import models.CurrentDataset
import play.api.libs.iteratee.Concurrent
import play.api.libs.Comet
import play.api.libs.Comet.CometMessage
import scala.util.Random
import supplychain.model._
import supplychain.model.Shipping
import supplychain.model.Order
import javax.xml.bind.DatatypeConverter
import supplychain.dataset.DatasetStatistics

object Application extends Controller {

  def print() {
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

    val stats = new DatasetStatistics(CurrentDataset())
    implicit val orderMessage = CometMessage[Order](d => s"'${d.connection.sender.id}', '${d.connection.id}', ${stats.dueParts(d.connection)}")
    implicit val shippingMessage = CometMessage[Shipping](d => s"'${d.connection.sender.id}', '${d.connection.id}', ${stats.dueParts(d.connection)}")

    val orderEnumeratee = orderEnumerator &> Comet(callback = "parent.addOrder")
    val shippingEnumeratee = shippingEnumerator &> Comet(callback = "parent.addShipping")
    val refreshMetrics = shippingEnumerator &> Comet(callback = "parent.refreshMetrics")

    Ok.chunked(orderEnumeratee interleave shippingEnumeratee interleave refreshMetrics)
  }

  def messages(supplierId: String) = Action {
    val downstreamMessages = CurrentDataset().messages.filter(_.connection.sender.id == supplierId)
    val upstreamMessages = CurrentDataset().messages.filter(_.connection.receiver.id == supplierId)

    Ok(views.html.messages(downstreamMessages, upstreamMessages))
  }

}