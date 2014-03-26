package dataset

case class Address(uri: String, name: String, street: String, zipcode: String, city: String, country: String, latitude: Double, longitude: Double) {

  def id = uri.substring(uri.lastIndexOf('/') + 1)
}