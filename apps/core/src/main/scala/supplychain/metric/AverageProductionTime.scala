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
     // Collect all times in a list
     var times = List[Double]()

     // Find all pairs of orders and shippings
     for(Order(orderDate, connection, count) +: tail <- messages.tails;
         Shipping(uri, shippingDate, connection, count) <- tail.find(_.connection.id == connection.id)) {

       // Parse times
       val orderDateParsed = DatatypeConverter.parseDateTime(orderDate)
       val shippingDateParsed = DatatypeConverter.parseDateTime(shippingDate)

       // Compute time difference
       val time = shippingDateParsed.getTimeInMillis - orderDateParsed.getTimeInMillis
       times ::= time
     }

     // Compute average
     times.sum / times.size / 1000.0
   }

 }
