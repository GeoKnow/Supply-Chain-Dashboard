package supplychain.metric

import supplychain.model.{Shipping, Order, Message}
import javax.xml.bind.DatatypeConverter

/**
 * Computes the average production time.
 */
class AverageProductionTime extends Metric {

   val dimension = "average production time"

   val unit = "days"

   def apply(messages: Seq[Message]): Double = {
     // Collect all production times
     val times = messages.collect { case s: Shipping => (s.date - s.order.date).milliseconds}
     // Compute average
     times.sum / times.size / 1000.0 / 60.0 / 60.0 / 24.0
   }
 }
