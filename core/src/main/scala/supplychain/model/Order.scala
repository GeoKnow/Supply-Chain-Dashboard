package supplychain.model

import supplychain.dataset.Namespaces
import java.util.UUID

case class Order(uri: String = Namespaces.message + UUID.randomUUID.toString,
                 date: DateTime,
                 connection: Connection,
                 count: Int) extends Message {

  // The sender of this message.
  override def sender = connection.target

  // The receiver of this message.
  override def receiver = connection.source

  val dueDate = date + (connection.content.productionTime + connection.shippingTime)
}
