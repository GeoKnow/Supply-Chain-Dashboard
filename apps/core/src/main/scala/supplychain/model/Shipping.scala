package supplychain.model

import java.util.UUID

import supplychain.dataset.Namespaces

case class Shipping(uri: String = Namespaces.message + UUID.randomUUID.toString,
                    date: DateTime,
                    connection: Connection,
                    count: Int,
                    order: Order,
                    woSource: WeatherObservation,
                    woTarget: WeatherObservation) extends Message {

  // The sender of this message.
  override def sender = connection.source

  // The receiver of this message.
  override def receiver = connection.target

}
