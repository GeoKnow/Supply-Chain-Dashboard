package supplychain.metric

import supplychain.model.{Shipping, Order, Message}

class DueParts extends Metric {

  override def dimension: String = "Parts due"

  override def unit: String = "Parts"

  override def apply(messages: Seq[Message]): Double = {
    val ordersParts = messages.collect{ case o: Order => o.count }.sum
    val shippedParts = messages.collect{ case s: Shipping => s.count }.sum

    ordersParts - shippedParts
  }
}
