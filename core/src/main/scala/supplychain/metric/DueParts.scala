package supplychain.metric

import java.util.logging.Logger

import supplychain.model.{Shipping, Order, Message}

class DueParts extends Metric {

  private val log = Logger.getLogger(getClass.getName)

  override def dimension: String = "Parts due"

  override def unit: String = "Parts"

  override def apply(messages: Seq[Message]): Double = {
    val ordersParts = messages.collect{ case o: Order => o.count }.sum
    val shippedParts = messages.collect{ case s: Shipping => s.count }.sum

    log.fine(ordersParts.toString + " - " + shippedParts + " based on #ofMessages: " + messages.size.toString)

    ordersParts - shippedParts
  }
}
