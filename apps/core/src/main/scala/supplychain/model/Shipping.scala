package supplychain.model

import supplychain.dataset.Namespaces
import java.util.UUID

case class Shipping(uri: String = Namespaces.message + UUID.randomUUID.toString,
                    date: DateTime,
                    connection: Connection,
                    count: Int,
                    order: Order) extends Message {

  // The sender of this message.
  override def sender = connection.source

  // The receiver of this message.
  override def receiver = connection.target

}
