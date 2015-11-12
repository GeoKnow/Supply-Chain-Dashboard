package supplychain.model

case class Supplier(uri: String, name: String, address: Address, coords: Coordinates, product: Product, weatherStation: WeatherStation, feature: String = "") {

  def id = uri.substring(uri.lastIndexOf('/') + 1)
}
