package controllers

import models._
import play.api.libs.Comet
import play.api.libs.Comet.CometMessage
import play.api.libs.iteratee.Concurrent
import play.api.mvc._
import supplychain.dataset.DatasetStatistics
import supplychain.metric.{Evaluator, Metrics}
import supplychain.model.{Order, Shipping, _}

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
    val messages = CurrentDataset().messages.filter(_.connection.source.id == supplierId)
    val supplier = CurrentDataset().suppliers.find(_.id == supplierId).get
    Ok(views.html.metrics(messages, supplier))
  }

  def news(supplierId: String) = Action {
    val ec = Configuration.get.endpointConfig
    val np = new NewsProvider(ec)
    val supplier = CurrentDataset().suppliers.find(_.id == supplierId)
    if (supplier.isDefined) {
      val date = RdfStoreDataset.Scheduler.currentDate
      val news = np.getNews(supplier.get, date)
      Ok(views.html.news(news, supplier.get))
    } else {
      NotFound
    }
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

}