package supplychain.simulator

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import scala.collection.mutable
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import supplychain.model._
import java.util.{Calendar, GregorianCalendar, UUID}
import javax.xml.datatype.DatatypeFactory
import supplychain.dataset.Namespaces
import supplychain.model.Shipping
import supplychain.model.Supplier
import supplychain.model.Order

/**
 * A supplier that builds products from parts that it receives from other suppliers.
 */
class SupplierActor(supplier: Supplier, simulator: Simulator) extends Actor {

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
      orderParts(date, count)

    case shipping @ Shipping(uri, date, connection, count, order) =>
      log.info("Received shipping of " + connection.content.name)
      simulator.addMessage(shipping)
      storage.put(connection.content, count)
      tryProduce()
  }

  private def orderParts(date: DateTime, count: Int): Unit = {
    val incomingConnections = simulator.connections.filter(_.receiver == supplier)
    for(connection <- incomingConnections) {
      val order =
        Order(
          date = date,
          connection = connection,
          count = count
        )
      simulator.getActor(connection.sender) ! order
    }
  }

  private def tryProduce(): Unit = {
    for(order <- orders.headOption) {
      if(storage.take(supplier.product.parts)) {
        orders.dequeue()
        // Determine shipping time
        val deliveryTime =
          if(Random.nextBoolean())
            supplier.product.productionTime + order.connection.shippingTime
          else {
            log.info(s"Shipping of ${supplier.product.name} will be delayed.")
            supplier.product.productionTime * 2 + order.connection.shippingTime
          }
        // Schedule shipping message
        context.system.scheduler.scheduleOnce((deliveryTime.milliseconds / simulator.scale).millis) {
          val shipping =
            Shipping(
              date = order.date + deliveryTime,
              connection = order.connection,
              count = order.count,
              order = order
            )
          simulator.getActor(order.connection.receiver) ! shipping
        }
      }
    }
  }
}