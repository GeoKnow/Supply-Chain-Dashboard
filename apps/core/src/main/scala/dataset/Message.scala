package dataset

/**
 * An EDI message. Currently either an Order or a Shipping.
 */
trait Message {

  def connection: Connection

}
