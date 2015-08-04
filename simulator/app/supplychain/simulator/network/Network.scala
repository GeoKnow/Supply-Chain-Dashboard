package supplychain.simulator.network

import java.util.logging.Logger

import supplychain.dataset.Namespaces
import supplychain.model._
import supplychain.simulator.network.Network._
import supplychain.simulator.{ConfigurationProvider, WeatherProvider}

/**
 * Represents a supply chain network.
 *
 * @param product The end product that is produced by this network.
 * @param suppliers List of suppliers in this network.
 * @param connections List of connections between suppliers.
 * @param rootConnection The root connection in the supply chain tree.
 */
case class Network(product: Product, suppliers: Seq[Supplier], connections: Seq[Connection], rootConnection: Connection) {

}

/**
 * Factory for building supply chain networks.
 */
object Network {

  private val log = Logger.getLogger(getClass.getName)

  /**
   * Builds a new network for a given product.
   * The network is built in two steps:
   * First, a list of suppliers is generated and than connections are generated between suppliers.
   */
  def build(product: Product, wp: WeatherProvider, cp:ConfigurationProvider): Network = {
    // Build Supplier List
    val supplierBuilder = new SupplierBuilder(wp, cp)
    val suppliers = supplierBuilder(product)
    log.info("related suppliers: ")
    for (s <- suppliers) {
      log.info(s.uri)
    }
    log.info("")
    // Build Network
    val networkBuilder = new ConnectionsBuilder(suppliers)
    val connections = networkBuilder(product)
    // Create an OEM that is responsible for sending the initial orders for the product
    val rootSupplier = generateRootSupplier(product, wp)
    val rootConnection = Connection(Namespaces.connection + "Initial", product, suppliers.head, rootSupplier)

    Network(product, rootSupplier +: suppliers, connections, rootConnection)
  }

  private def generateRootSupplier(product: Product, wp: WeatherProvider) = {
    val crds = Coordinates(0.0, 0.0)
    val ws = wp.getNearesWeaterStation(crds)
    Supplier(
      uri = Namespaces.supplier + "OEM",
      name = "OEM",
      address = Address("", "", "", ""),
      coords = crds,
      Product("OEM", parts = product :: Nil),
      ws
    )
  }
}
