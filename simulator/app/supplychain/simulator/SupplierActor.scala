package supplychain.simulator

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import supplychain.dataset.WeatherProvider
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
class SupplierActor(supplier: Supplier, simulator: Simulator, wp: WeatherProvider) extends Actor {

  val delayProbability = Random.nextDouble() * 0.5

  var demandForecast = 1

  val demandTimeframe = Duration.days(30)

  private val storage = new Storage(supplier.product.parts)

  private val orders = mutable.Queue[Order]()

  private val log = Logging(context.system, this)

  //private val weatherProvider = new WeatherProvider

  def hasMessages: Boolean = {
    orders.nonEmpty
  }

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
      simulator.scheduleMessage(
        Order(
          date = date,
          connection = connection,
          count = connection.content.count * count
        )
      )
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

        // Weather influence
        val delayedDueToWeatherProbability = wp.delayedDueToWeatherProbability(supplier.weatherStation, order.dueDate, Simulator().startDate, Simulator().simulationEndDate) + (0.05 * Random.nextDouble())

        // Production time, 10% fixed delay
        var productionTime = supplier.product.productionTime
        if (Random.nextDouble() <= 0.1) {
          productionTime = productionTime * (1.1 + Random.nextDouble())
        }

        // Shipping time, delay due to weather depended on date (and location)
        var shippingTime = order.connection.shippingTime
        if (Random.nextDouble() <= delayedDueToWeatherProbability){
          shippingTime = shippingTime * (1.1 + Random.nextInt(3))
        }

        // Delivery Times
        val perfectDeliveryTime = supplier.product.productionTime + order.connection.shippingTime
        val realDeliveryTime = productionTime + shippingTime

        // Number of parts delayed
        val partsDelayed = 1 + Random.nextInt(order.count)
        val partsOnTime = order.count - partsDelayed

        // Schedule shipping messages
        //val productionDelayed = Random.nextDouble() <= delayedDueToWeatherProbability
        if(realDeliveryTime > perfectDeliveryTime) {
          ship(order, partsOnTime, perfectDeliveryTime)
          ship(order, partsDelayed, realDeliveryTime)
        } else {
          ship(order, order.count, realDeliveryTime)
        }
      }
    }
  }

  /**
   * Schedules the shipping of parts.
   */
  private def ship(order: Order, count: Int, deliveryTime: Duration) = {
    val shippingDate = order.date + deliveryTime
    if(count >= 1) {
      simulator.scheduleMessage(
        Shipping(
          date = shippingDate,
          connection = order.connection,
          count = count,
          order = order
        )
      )
    }
  }
}