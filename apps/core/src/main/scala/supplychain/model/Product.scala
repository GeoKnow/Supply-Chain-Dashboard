package supplychain.model

import supplychain.dataset.Namespaces
import java.util.UUID
import scala.concurrent.duration.FiniteDuration
import scala.util.Random

/**
 * Specifies a product.
 *
 * @param name Human-readable name for this product, e.g., windshield
 * @param count
 * @param parts List of parts needed for this product.
 * @param uri Unique URI for this product, will be generated automatically if not provided.
 */
case class Product(name: String, count: Int = 1, parts: List[Product] = Nil, uri: String = Namespaces.product + UUID.randomUUID().toString) {

  // Production time in days
  val productionTime = Duration.days(0.1 + 0.4 * Random.nextDouble())

  /**
   * Generates a list of all parts of this product, including parts of parts.
   */
  def partList: List[Product] = parts ::: parts.flatMap(_.partList)
}
