package supplychain.simulator.network

import java.util.UUID
import supplychain.dataset.Namespaces
import supplychain.model.{Address, Coordinates, Supplier, Product}
import supplychain.simulator.WeatherProvider_
import scala.util.Random

class SupplierBuilder(wp: WeatherProvider_) {

  private val random = new Random(0)

  def apply(product: Product): Seq[Supplier] = {
    for(part <- product :: product.partList) yield generateSupplier(part)
  }

  private def generateSupplier(product: Product) = {
    val lat = 47 + 7 * random.nextDouble()
    val lon = 6 + 9 * random.nextDouble()
    val coordinates = Coordinates(lat, lon)
    val address = Address("Beispielstr. 1", "10123", "Musterstadt", "Deutschland")
    val ws = wp.getNearesWeaterStation(coordinates)
    Supplier(
      Namespaces.supplier + product.name + "-" + UUID.randomUUID.toString,
      product.name + " Supplier", address, coordinates, product, ws)
  }
}
