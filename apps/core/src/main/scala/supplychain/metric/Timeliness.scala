package supplychain.metric

import supplychain.model.{Shipping, Message}

class Timeliness extends Metric {
  /** The dimension that is measured, e.g., ''average production time'' */
  override def dimension: String = "Timeliness"

  /** The unit of the returned values, e.g., ''seconds'' */
  override def unit: String = "%"

  /** Computes this metric for a specific network. */
  override def apply(messages: Seq[Message]): Double = {
    // Collect all production times
    val times = messages.collect{ case s: Shipping => s.date <= s.order.dueDate }
    // Compute average
    if(!times.isEmpty)
      times.count(identity).toDouble / times.size * 100.0
    else
      100.0
  }
}
