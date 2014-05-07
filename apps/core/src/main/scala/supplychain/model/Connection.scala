package supplychain.model

case class Connection(uri: String, content: Product, sender: Supplier, receiver: Supplier) {

  def id = uri.substring(uri.lastIndexOf('/') + 1)

  override def toString = s"Connection($id, ${content.name}, ${sender.name}, ${receiver.name})"
}
