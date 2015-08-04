package supplychain.model

/**
 * An EDI message. Currently either an Order or a Shipping.
 */
trait Message {

  // The URI of this message.
  def uri: String

  // The sending date.
  def date: DateTime

  // The corresponding connection.
  def connection: Connection

  // The sender of this message.
  def sender: Supplier

  // The receiver of this message.
  def receiver: Supplier

}
