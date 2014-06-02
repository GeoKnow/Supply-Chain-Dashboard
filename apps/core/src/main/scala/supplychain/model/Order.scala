package supplychain.model

case class Order(uri: String, date: String, connection: Connection, count: Int) extends Message {

}
