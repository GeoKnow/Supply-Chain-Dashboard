package supplychain.simulator

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import scala.collection.mutable
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import supplychain.model._
import supplychain.model.Duration
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory
import supplychain.model.Shipping
import supplychain.model.Supplier
import supplychain.model.Order

/**
 * A supplier that builds products from parts that it receives from other suppliers.
 */
class SupplierActor(supplier: Supplier, simulator: Simulator) extends Actor {

  val delayProbability = Random.nextDouble() * 0.5

  var demandForecast = 1

  val demandTimeframe = Duration.days(30)

  private val storage = new Storage(supplier.product.parts)

  private val orders = mutable.Queue[Order]()

  private val log = Logging(context.system, this)

  /**
   * Receives and processes messages.
   */
  def receive = {
    case ord @ Order(uri, date, connection, count) =>
      log.info("Received order of " + supplier.product.name)
      simulator.addMessage(ord)
      orders.enqueue(ord)
      produce()
      order(date, count)

    case ship @ Shipping(uri, date, connection, count, order) =>
      log.info("Received shipping of " + connection.content.name)
      simulator.addMessage(ship)
      storage.put(connection.content, count)
      produce()
  }

  /**
   * Schedules new orders.
   */
  private def order(date: DateTime, count: Int): Unit = {
    val incomingConnections = simulator.connections.filter(_.target == supplier)
    for(connection <- incomingConnections) {
      val order =
        Order(
          date = date,
          connection = connection,
          count = connection.content.count * count
        )
      simulator.getActor(connection.source) ! order
    }
  }

  /**
   * Produces parts (if needed and possible).
   */
  private def produce(): Unit = {
    for (order <- orders.headOption) {
      if (storage.take(supplier.product.parts)) {
        // Remove order
        orders.dequeue()
        // Delivery times
        val delayFactor = 0.1 + Random.nextDouble()
        val deliveryTime = supplier.product.productionTime + order.connection.shippingTime
        val delayedDeliveryTime = supplier.product.productionTime * (1.0 + delayFactor) + order.connection.shippingTime
        // Number of parts delayed
        val partsDelayed = 1 + Random.nextInt(order.count)
        val partsOnTime = order.count - partsDelayed
        // Schedule shipping messages
        val productionDelayed = Random.nextDouble() <= delayProbability
        if(productionDelayed) {
          ship(order, partsOnTime, deliveryTime)
          ship(order, partsDelayed, delayedDeliveryTime)
        } else {
          ship(order, order.count, deliveryTime)
        }
      }
    }
  }

  /**
   * Schedules the shipping of parts.
   */
  private def ship(order: Order, count: Int, deliveryTime: Duration) = {
    if(count >= 1) {
      context.system.scheduler.scheduleOnce((deliveryTime.milliseconds / simulator.scale).millis) {
        val shipping =
          Shipping(
            date = order.date + deliveryTime,
            connection = order.connection,
            count = count,
            order = order
          )
        simulator.getActor(order.connection.target) ! shipping
      }
    }
  }
}