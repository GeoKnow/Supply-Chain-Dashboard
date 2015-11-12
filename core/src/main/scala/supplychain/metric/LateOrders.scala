package supplychain.metric

import java.util.logging.Logger

import supplychain.model.{DateTime, Message, Order, Shipping}

class LateOrders extends Metric {

  private val log = Logger.getLogger(getClass.getName)

  override def dimension: String = "Orders Late"

  override def unit: String = "Orders"

  override def apply(messages: Seq[Message], currentDate: DateTime): Double = {

    val orders = messages.collect{ case o: Order => o }
    val shippings = messages.collect{ case s: Shipping => s }.groupBy(_.order.uri)

    def hasBeenShipped(o: Order): Boolean = {
      shippings.get(o.uri) match {
        case Some(shippings) => shippings.map(_.count).sum >= o.count
        case None => false
      }
    }

    orders.filterNot(hasBeenShipped).filter(_.dueDate < currentDate).size
  }
}
