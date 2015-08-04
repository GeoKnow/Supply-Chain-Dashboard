package supplychain.simulator.network

import supplychain.model.{Product, Supplier}
import supplychain.simulator.{ConfigurationProvider, WeatherProvider}

class SupplierBuilder(wp: WeatherProvider, cp: ConfigurationProvider) {

  //private val random = new Random(0)

  def apply(product: Product): Seq[Supplier] = {
    for(part <- product :: product.partList) yield cp.getSupplier(part)
  }

  /*
  private def generateSupplier(product: Product) = {
    cp.getSupplier(product)
//    val lat = 47 + 7 * random.nextDouble()
//    val lon = 6 + 9 * random.nextDouble()
//    val coordinates = Coordinates(lat, lon)
//    val address = Address("Beispielstr. 1", "10123", "Musterstadt", "Deutschland")
//    val ws = wp.getNearesWeaterStation(coordinates)
//    Supplier(
//    Namespaces.supplier + product.name + "-" + UUID.randomUUID.toString,
//    product.name + " Supplier", address, coordinates, product, ws)
  }
  */
}
