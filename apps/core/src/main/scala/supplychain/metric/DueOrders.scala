package supplychain.metric

import supplychain.model.{Shipping, Order, Message}

class DueOrders extends Metric {

  override def dimension: String = "Orders due"

  override def unit: String = "Orders"

  override def apply(messages: Seq[Message]): Double = {
    messages.count(_.isInstanceOf[Order]) - messages.count(_.isInstanceOf[Shipping])
  }
}
