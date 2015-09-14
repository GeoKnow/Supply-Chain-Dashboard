package supplychain.model

import supplychain.dataset.Namespaces

import scala.util.Random

case class Connection(uri: String, content: Product, source: Supplier, target: Supplier) {

  def id = uri.substring(uri.lastIndexOf('/') + 1)

  private val myRand = new Random(uri.hashCode)

  val shippingTime = Duration.days(1.0 + 1.0 * myRand.nextDouble())

  override def toString = s"Connection($id, ${content.name}, ${source.name}, ${target.name})"
}
