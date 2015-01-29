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

    if (product.name.trim.toLowerCase.startsWith("optic")) {
      val coordinates = Coordinates(50.8853721, 11.5895545)
      val address = Address("Otto-Eppenstein-Strasse 2", "07745", "Jena", "Deutschland")
      val ws = wp.getNearesWeaterStation(coordinates)
      val logo = "http://www.opticsbalzers.com/images/default/design/head/logo.gif"
      Supplier(
        Namespaces.supplier + product.name + "-" + UUID.randomUUID.toString,
        product.name, address, coordinates, product, ws, logo)
    } else if (product.name.trim.toLowerCase.startsWith("pls")) {
      val coordinates = Coordinates(49.15721,9.21697)
      val address = Address("Salzstraße 94", "74076", "Heilbronn", "Deutschland")
      val ws = wp.getNearesWeaterStation(coordinates)
      val logo = "http://www.pls-hn.de/wpimages/wp4c75040b_06.png"
      Supplier(
        Namespaces.supplier + product.name + "-" + UUID.randomUUID.toString,
        product.name, address, coordinates, product, ws, logo)
    } else if (product.name.trim.toLowerCase.startsWith("getrag")) {
      val coordinates = Coordinates(49.49698, 10.40923)
      val address = Address("Burgbernheimer Straße 5 ", "91438", "Bad Windsheim", "Deutschland")
      val ws = wp.getNearesWeaterStation(coordinates)
      val logo = "http://www.getrag.com/static/img/layout/getrag_logo_high.png"
      Supplier(
        Namespaces.supplier + product.name + "-" + UUID.randomUUID.toString,
        product.name, address, coordinates, product, ws, logo)
    } else if (product.name.trim.toLowerCase.startsWith("allgaier")) {
      val coordinates = Coordinates(48.707097, 9.594437)
      val address = Address("Ulmer Straße 75", "73066", "Uhingen", "Deutschland")
      val ws = wp.getNearesWeaterStation(coordinates)
      val logo = "http://www.allgaier.de/sites/all/themes/allgaier/logo.png"
      Supplier(
        Namespaces.supplier + product.name + "-" + UUID.randomUUID.toString,
        product.name, address, coordinates, product, ws, logo)
    } else {
      val coordinates = Coordinates.nextRandCoord()
      val address = Address("Beispielstr. 1", "10123", "Musterstadt", "Deutschland")
      val ws = wp.getNearesWeaterStation(coordinates)
      Supplier(
        Namespaces.supplier + product.name + "-" + UUID.randomUUID.toString,
        product.name, address, coordinates, product, ws)
    }
  }
}
