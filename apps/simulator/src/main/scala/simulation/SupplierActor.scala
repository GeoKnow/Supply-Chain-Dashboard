package simulation

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import scala.collection.mutable
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import SupplierActor._
import dataset.{Shipping, Supplier, Connection}
import java.util.{GregorianCalendar, UUID}
import javax.xml.datatype.DatatypeFactory

/**
 * A supplier that builds products from parts that it receives from other suppliers.
 */
class SupplierActor(supplier: Supplier, simulation: Simulation) extends Actor {

  val productionTime: FiniteDuration = 1.seconds + Random.nextInt(10).seconds

  val delayProbability = Random.nextDouble() * 0.7

  private val storage = new Storage(supplier.product.parts)

  private val orders = mutable.Queue[Order]()

  private val log = Logging(context.system, this)

  def receive = {
    case OrderMsg(count) =>
      log.info("Received order for " + supplier.product.name)
      orderParts(count)
      orders.enqueue(Order(sender, supplier.product, count))
      tryProduce()

    case ShippingMsg(connection, count) =>
      log.info("Received shipping of " + connection.content.name)
      storage.put(connection.content, count)
      simulation.addShipping(
        Shipping(
          uri = UUID.randomUUID.toString,
          date = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()).toXMLFormat,
          count = count,
          connection = connection
        )
      )
      tryProduce()
  }

  private def orderParts(count: Int): Unit = {
    val incomingConnections = simulation.connections.filter(_.receiver == supplier)
    for(connection <- incomingConnections)
      simulation.getActor(connection.sender) ! OrderMsg(connection.content.count * count)
  }

  private def tryProduce(): Unit = {
    for(order <- orders.headOption) {
      if(storage.take(order.product.parts)) {
        orders.dequeue()
        // Determine production time
        val time =
          if(Random.nextBoolean())
            productionTime
          else {
            log.info(s"Shipping of ${order.product.name} will be delayed.")
            productionTime * 2
          }
        // Schedule shipping message
        val connection = simulation.connections.find(_.sender != supplier).get
        context.system.scheduler.scheduleOnce(time, order.sender, ShippingMsg(connection, order.count))
      }
    }
  }
}

object SupplierActor {

  case class Order(sender: ActorRef, product: dataset.Product, count: Int)

  case class OrderMsg(count: Int)

  case class ShippingMsg(connection: Connection, count: Int)
}