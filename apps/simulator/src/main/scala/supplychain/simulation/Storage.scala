package supplychain.simulation

import scala.collection.mutable
import supplychain.model.Product

/**
 * A storage of parts.
 *
 * @param products The type of products that are stored.
 */
class Storage(products: List[Product]) {

  private val storage = mutable.Map[Product, Int]()
  for(product <- products)
    storage(product) = 0

  /**
   * Puts a new product into the storage.
   *
   * @param product The product.
   * @param count The number of products to add to the storage.
   */
  def put(product: supplychain.model.Product, count: Int): Unit = {
    storage(product) += count
  }

  /**
   * Asks how many units of a particular product are available.
   *
   * @param product The Product.
   * @return The number of available units.
   */
  def ask(product: Product): Int = {
    storage(product)
  }

  /**
   * Retrieves parts from the storage.
   *
   * @param parts The parts to remove from the storage
   * @return True, if all parts have been available and have been taken.
   *         False, if at least one part was missing and no parts have been taken.
   */
  def take(parts: List[Product]): Boolean = {
    if(parts.forall(p => storage(p) >= p.count )) {
      for (part <- parts)
        storage(part) -= part.count
      true
    } else {
      false
    }
  }
}