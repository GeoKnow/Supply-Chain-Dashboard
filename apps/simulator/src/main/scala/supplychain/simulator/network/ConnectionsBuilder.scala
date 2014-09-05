package supplychain.simulator.network

import java.util.UUID

import supplychain.dataset.Namespaces
import supplychain.model._
import supplychain.simulator.WeatherProvider

class ConnectionsBuilder(suppliers: Seq[Supplier]) {

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
    val wsSource = WeatherProvider.getNearesWeaterStation(partSupplier)
      //new WeatherStation(partSupplier.coords, partSupplier.name + WeatherUtil.WS_NAME_SUFIX)
    val wsTarget = WeatherProvider.getNearesWeaterStation(supplier)
      //new WeatherStation(supplier.coords, supplier.name + WeatherUtil.WS_NAME_SUFIX)
    val connection = Connection(Namespaces.connection + UUID.randomUUID.toString, part, partSupplier, supplier, wsSource, wsTarget)
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
