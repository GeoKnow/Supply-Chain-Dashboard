package supplychain.model

import supplychain.dataset.Namespaces
import java.util.UUID

case class Order(uri: String = Namespaces.message + UUID.randomUUID.toString,
                 date: DateTime,
                 connection: Connection,
                 count: Int) extends Message {

  val dueDate = date + ((connection.content.productionTime + connection.shippingTime) * 1.2)
}
