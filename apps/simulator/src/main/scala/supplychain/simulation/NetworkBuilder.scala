package supplychain.simulation

import supplychain.model.{Connection, Supplier, Product}
import java.util.UUID

trait NetworkBuilder {
  def apply(product: Product)
}

class SimpleNetworkBuilder(suppliers: Seq[Supplier]) {

  // A map that returns all suppliers to a given product
  private val supplierMap = suppliers.groupBy(_.product)

  /**
   * Generates all connections for producing a particular product.
   */
  def apply(product: Product): List[Connection] = {
    val productSupplier = selectSupplier(product)
    product.parts.flatMap(generate(productSupplier, _))
  }

  /**
   * Generates incoming connection to a specific supplier
   * @param supplier The supplier
   * @param part The part that is needed by the given supplier
   * @return A list of connections
   */
  protected def generate(supplier: Supplier, part: Product): List[Connection] = {
    val partSupplier = selectSupplier(part)
    val connection = Connection(UUID.randomUUID.toString, part, partSupplier, supplier)
    connection :: part.parts.flatMap(generate(partSupplier, _))
  }

  /**
   * Selects a supplier for a specific part.
   */
  protected def selectSupplier(part: Product) = {
    // At the moment we just choose the first supplier
    supplierMap(part).head
  }

}
