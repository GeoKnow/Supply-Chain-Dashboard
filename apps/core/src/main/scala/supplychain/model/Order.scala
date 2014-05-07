package supplychain.model

case class Order(date: String, connection: Connection, count: Int) extends Message {

}
