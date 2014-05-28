package supplychain.model

/**
 * An EDI message. Currently either an Order or a Shipping.
 */
trait Message {

  def uri: String

  def date: String

  def connection: Connection

}
