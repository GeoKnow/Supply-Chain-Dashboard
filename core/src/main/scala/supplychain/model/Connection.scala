package supplychain.model

import supplychain.dataset.Namespaces

import scala.util.Random

case class Connection(uri: String, content: Product, source: Supplier, target: Supplier) {

  def id = uri.substring(uri.lastIndexOf('/') + 1)

  val shippingTime = Duration.days(1.0 + 1.0 * Random.nextDouble())

  override def toString = s"Connection($id, ${content.name}, ${source.name}, ${target.name})"
}