package supplychain.metric

import supplychain.model.{Shipping, Message}
import javax.xml.bind.DatatypeConverter

class AverageDelay extends Metric {
  /** The dimension that is measured, e.g., ''average production time'' */
  override def dimension: String = "Average delay"

  /** The unit of the returned values, e.g., ''seconds'' */
  override def unit: String = "days"

  /** Computes this metric for a specific network. */
  override def apply(messages: Seq[Message]): Double = {
    // Collect all production times
    val times = messages.collect { case s: Shipping => (s.date - s.order.dueDate).milliseconds}
    // Compute average
    if(!times.isEmpty)
      times.sum / times.size / 1000.0 / 60.0 / 60.0 / 24.0
    else
      0.0
  }
}
