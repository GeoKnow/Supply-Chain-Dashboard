package supplychain.simulator

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import scala.collection.mutable
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import supplychain.model.{Order, Shipping, Supplier, Connection}
import java.util.{GregorianCalendar, UUID}
import javax.xml.datatype.DatatypeFactory
import supplychain.model.Supplier
import supplychain.dataset.Namespaces

/**
 * A supplier that builds products from parts that it receives from other suppliers.
 */
class SupplierActor(supplier: Supplier, simulator: Simulator) extends Actor {

  val productionTime: FiniteDuration = 1.seconds + Random.nextInt(10).seconds

  val delayProbability = Random.nextDouble() * 0.5

  private val storage = new Storage(supplier.product.parts)

  private val orders = mutable.Queue[Order]()

  private val log = Logging(context.system, this)

  def receive = {
    case order @ Order(uri, date, connection, count) =>
      log.info("Received order of " + supplier.product.name)
      simulator.addMessage(order)
      orders.enqueue(order)
      tryProduce()
      orderParts(count)

    case shipping @ Shipping(uri, date, connection, count) =>
      log.info("Received shipping of " + connection.content.name)
      simulator.addMessage(shipping)
      storage.put(connection.content, count)
      tryProduce()
  }

  private def orderParts(count: Int): Unit = {
    val incomingConnections = simulator.connections.filter(_.receiver == supplier)
    for(connection <- incomingConnections)
      simulator.getActor(connection.sender) ! Order(uri(), date(), connection, connection.content.count * count)
  }

  private def tryProduce(): Unit = {
    for(order <- orders.headOption) {
      if(storage.take(supplier.product.parts)) {
        orders.dequeue()
        // Determine production time
        val time =
          if(Random.nextBoolean())
            productionTime
          else {
            log.info(s"Shipping of ${supplier.product.name} will be delayed.")
            productionTime * 2
          }
        // Schedule shipping message
        val shipping =
          Shipping(
            uri = uri(),
            date = date(),
            connection = order.connection,
            count = order.count
          )
        context.system.scheduler.scheduleOnce(time) {
          simulator.getActor(order.connection.receiver) ! shipping
        }
      }
    }
  }

  // Generates a new message URI
  def uri() = Namespaces.message +  UUID.randomUUID.toString

  // The current date as xsd:date
  def date() = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()).toXMLFormat
}