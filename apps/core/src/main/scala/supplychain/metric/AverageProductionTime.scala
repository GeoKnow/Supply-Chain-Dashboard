package supplychain.metric

import supplychain.model.{Shipping, Order, Message}
import javax.xml.bind.DatatypeConverter

/**
 * Computes the average production time.
 */
class AverageProductionTime extends Metric {

   val dimension = "average production time"

   val unit = "seconds"

   def apply(messages: Seq[Message]): Double = {
     // Collect all production times
     val times = messages.collect{ case s: Shipping => productionTime(s) }
     // Compute average
     times.sum / times.size / 1000.0
   }

   private def productionTime(shipping: Shipping) = {
     // Parse dates
     val orderDateParsed = DatatypeConverter.parseDateTime(shipping.order.date)
     val shippingDateParsed = DatatypeConverter.parseDateTime(shipping.date)
     // Compute time difference
     shippingDateParsed.getTimeInMillis - orderDateParsed.getTimeInMillis
   }

 }
