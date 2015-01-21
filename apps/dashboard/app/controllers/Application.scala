package controllers

import play.api.mvc.{Controller, Action}
import models.{CurrentMetrics, CurrentDataset}
import play.api.libs.iteratee.Concurrent
import play.api.libs.Comet
import play.api.libs.Comet.CometMessage
import supplychain.metric.{Metrics, Evaluator}
import scala.io.{Codec, Source}
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

  def metrics(supplierId: String) = Action {
    val messages = CurrentDataset().messages.filter(_.connection.target.id == supplierId)
    val supplier = CurrentDataset().suppliers.find(_.id == supplierId).get
    Ok(views.html.metrics(messages, supplier))
  }

  def report(supplierId: String) = Action {
    val scoreTable = Evaluator.table(CurrentDataset(), Metrics.all, supplierId)
    Ok(views.html.report(scoreTable))
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
    implicit val orderMessage = CometMessage[Order](d => s"'${d.connection.source.id}', '${d.connection.id}', ${stats.dueParts(d.connection)}")
    implicit val shippingMessage = CometMessage[Shipping](d => s"'${d.connection.source.id}', '${d.connection.id}', ${stats.dueParts(d.connection)}")

    val orderEnumeratee = orderEnumerator &> Comet(callback = "parent.addOrder")
    val shippingEnumeratee = shippingEnumerator &> Comet(callback = "parent.addShipping")
    val refreshMetrics = shippingEnumerator &> Comet(callback = "parent.refreshMetrics")

    Ok.chunked(orderEnumeratee interleave shippingEnumeratee interleave refreshMetrics)
  }

  def messages(supplierId: String) = Action {
    val downstreamMessages = CurrentDataset().messages.filter(_.connection.source.id == supplierId)
    val upstreamMessages = CurrentDataset().messages.filter(_.connection.target.id == supplierId)

    Ok(views.html.messages(downstreamMessages, upstreamMessages))
  }

  def xybermotive(supplierId: String) = Action {
    //val supplier = CurrentDataset().suppliers.find(_.id == supplierId).get

    val url = new java.net.URL("http://217.24.49.173/Bestand.Txt")
    val conn = url.openConnection()
    val stream = conn.getInputStream

    val source = Source.fromInputStream(stream)(Codec.ISO8859)

    val xyData = XybermotiveData(source.getLines().toList.drop(1).filter(!_.trim().isEmpty).map(XybermotiveInventory.parseLine))

    source.close()

    Ok(views.html.xybermotive(xyData))
  }

}