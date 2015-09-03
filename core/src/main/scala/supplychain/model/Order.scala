package supplychain.model

import play.api.libs.json.{Json, Writes}
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

object Order {
  implicit val simulationUpdateWrites = new Writes[Order] {
    def writes(o: Order) = Json.obj(
      "uri" -> o.uri,
      "date" -> o.date.toXSDFormat,
      "dueDate" -> o.dueDate.toXSDFormat,
      "connection" -> o.connection.id,
      "count" -> o.count,
      "connectionSourceId" -> o.connection.source.id,
      "connectionTargetId" -> o.connection.target.id
    )
  }
}

//supplierId, dueParts
