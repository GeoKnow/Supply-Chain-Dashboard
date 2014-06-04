package supplychain.model

case class Shipping(uri: String, date: String, connection: Connection, count: Int, order: Order) extends Message {

}
