package supplychain.model

import scala.util.Random

case class Connection(uri: String, content: Product, sender: Supplier, receiver: Supplier) {

  def id = uri.substring(uri.lastIndexOf('/') + 1)

  val shippingTime = Duration.days(1.0 + 3.0 * Random.nextDouble())

  override def toString = s"Connection($id, ${content.name}, ${sender.name}, ${receiver.name})"
}
