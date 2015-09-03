package supplychain.model

import java.util.UUID

import play.api.libs.json.{Json, Writes}
import supplychain.dataset.Namespaces

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

object Shipping {
  implicit val shippingWrites = new Writes[Shipping] {
  def writes(s: Shipping) = Json.obj(
    "uri" -> s.uri,
    "date" -> s.date.toXSDFormat,
    "connection" -> s.connection.id,
    "count" -> s.count,
    "order" -> s.order.uri,
    "connectionSourceId" -> s.connection.source.id,
    "connectionTargetId" -> s.connection.target.id
  )
  }
}
