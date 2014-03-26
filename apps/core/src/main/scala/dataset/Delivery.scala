package dataset

case class Delivery(uri: String, date: String, content: String, count: Int, unloadingPoint: String, sender: Address, receiver: Address) {

  def id = uri.substring(uri.lastIndexOf('/') + 1)
}
