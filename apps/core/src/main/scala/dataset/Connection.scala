package dataset

case class Connection(uri: String, content: Product, sender: Supplier, receiver: Supplier) {

  def id = uri.substring(uri.lastIndexOf('/') + 1)
}
